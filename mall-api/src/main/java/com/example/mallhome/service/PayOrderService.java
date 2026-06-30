package com.example.mallhome.service;

import com.example.mallhome.client.MallhomePayClient;
import com.example.mallhome.client.PaymentGatewayClient;
import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.domain.CreatePayOrderRequest;
import com.example.mallhome.domain.CreatePayOrderResponse;
import com.example.mallhome.domain.PayChannel;
import com.example.mallhome.domain.PayRuntimeConfig;
import com.example.mallhome.domain.PaymentOrderStatus;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.repository.ProductOrderRepository;
import com.example.mallhome.util.ClientIpUtils;
import com.example.mallhome.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.TreeMap;

@Service
public class PayOrderService {

    private final MallhomePayProperties properties;
    private final MallhomePayClient payClient;
    private final PaymentGatewayClient gatewayClient;
    private final PayRuntimeConfigService runtimeConfigService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ProductOrderRepository productOrderRepository;
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int ORDER_CREATE_MAX_RETRY = 5;

    public PayOrderService(
            MallhomePayProperties properties,
            MallhomePayClient payClient,
            PaymentGatewayClient gatewayClient,
            PayRuntimeConfigService runtimeConfigService,
            PaymentOrderRepository paymentOrderRepository,
            ProductOrderRepository productOrderRepository
    ) {
        this.properties = properties;
        this.payClient = payClient;
        this.gatewayClient = gatewayClient;
        this.runtimeConfigService = runtimeConfigService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.productOrderRepository = productOrderRepository;
    }

    public CreatePayOrderResponse createPayOrder(CreatePayOrderRequest request, HttpServletRequest servletRequest) {
        validateProductOrder(request);
        PayRuntimeConfig config = runtimeConfigService.getConfigForMerchant(request.getMerchantNo());
        String clientIp = ClientIpUtils.resolveClientIp(servletRequest);
        PaymentOrder order = createAndSaveLocalOrder(request, clientIp, config);
        String orderNo = order.getOrderNo();

        if (PayChannel.PAYMENT_GATEWAY.equals(config.getPayChannel())) {
            return createGatewayPayOrder(request, servletRequest, config, order);
        }
        return createMallhomePayOrder(request, config, order, orderNo, clientIp);
    }

    private CreatePayOrderResponse createMallhomePayOrder(
            CreatePayOrderRequest request,
            PayRuntimeConfig config,
            PaymentOrder order,
            String orderNo,
            String clientIp
    ) {
        // 组装平台支付接口要求的表单参数，字段名需要和接口文档保持一致。
        Map<String, String> params = new TreeMap<>();
        params.put("merTradeNo", orderNo);
        params.put("typeIndex", String.valueOf(request.getTypeIndex()));
        params.put("externalId", config.getExternalId());
        params.put("totalAmount", request.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("merSubject", request.getSubject());
        params.put("goodsType", String.valueOf(request.getGoodsType()));
        params.put("merPayNotifyUrl", config.getNotifyUrl());
        params.put("clientIp", clientIp);
        params.put("payMethodType", firstNonBlank(request.getPayMethodType(), config.getDefaultPayMethodType()));

        putIfHasText(params, "attachInfo", request.getAttachInfo());
        putIfHasText(params, "quitUrl", request.getQuitUrl());
        putIfHasText(params, "returnUrl", firstNonBlank(request.getReturnUrl(), config.getReturnUrl()));
        putIfHasText(params, "subExternalId", request.getSubExternalId());

        try {
            String rawResponse = payClient.operPay(config.getHost(), params);
            // 平台创建成功后，把 payUrl、平台单号等关键信息回写到本地订单表。
            applyCreateResponse(order, rawResponse);
            paymentOrderRepository.save(order);
            return new CreatePayOrderResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getStatus(),
                    order.getPayUrl(),
                    order.getPlatTradeNo(),
                    rawResponse
            );
        } catch (RuntimeException exception) {
            // 调用平台异常时也记录失败状态，便于后台排查和前端查询。
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            order.setPlatformCreateResponse(exception.getMessage());
            paymentOrderRepository.save(order);
            throw exception;
        }
    }

    private CreatePayOrderResponse createGatewayPayOrder(
            CreatePayOrderRequest request,
            HttpServletRequest servletRequest,
            PayRuntimeConfig config,
            PaymentOrder order
    ) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("merchantOrderNo", order.getOrderNo());
        payload.put("subject", request.getSubject());
        payload.put("totalAmount", request.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        payload.put("returnUrl", firstNonBlank(request.getReturnUrl(), config.getGatewayReturnUrl()));
        putIfHasTextObject(payload, "customerDisplayName", request.getCustomerDisplayName());
        if (request.getCustomerUserId() != null) {
            payload.put("customerUserId", request.getCustomerUserId());
        }
        putIfHasTextObject(payload, "businessNotifyUrl", config.getGatewayBusinessNotifyUrl());

        try {
            String gatewayRawResponse = gatewayClient.createWapPay(
                    config.getGatewayHost(),
                    config.getGatewayAppId(),
                    config.getGatewayAppSecret(),
                    payload
            );
            applyGatewayCreateResponse(order, gatewayRawResponse, buildPayPageUrl(servletRequest, order.getOrderNo()));
            paymentOrderRepository.save(order);
            return new CreatePayOrderResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getStatus(),
                    order.getPayUrl(),
                    order.getPlatTradeNo(),
                    order.getPlatformCreateResponse()
            );
        } catch (RuntimeException exception) {
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            order.setPlatformCreateResponse(exception.getMessage());
            paymentOrderRepository.save(order);
            throw exception;
        }
    }

    private PaymentOrder createAndSaveLocalOrder(CreatePayOrderRequest request, String clientIp, PayRuntimeConfig config) {
        for (int i = 0; i < ORDER_CREATE_MAX_RETRY; i++) {
            String orderNo = generateOrderNo();
            PaymentOrder order = createLocalOrder(request, orderNo, clientIp, config);
            try {
                // 先保存本地订单，再调用平台。数据库唯一键兜底订单号并发撞号。
                return paymentOrderRepository.saveAndFlush(order);
            } catch (DataIntegrityViolationException exception) {
                if (i == ORDER_CREATE_MAX_RETRY - 1) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("商户订单号生成失败，请重试");
    }

    private PaymentOrder createLocalOrder(CreatePayOrderRequest request, String orderNo, String clientIp, PayRuntimeConfig config) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setProductOrderId(request.getProductOrderId());
        order.setExternalId(firstNonBlank(
                request.getMerchantNo(),
                PayChannel.PAYMENT_GATEWAY.equals(config.getPayChannel()) ? config.getGatewayAppId() : config.getExternalId()
        ));
        order.setCustomerUserId(request.getCustomerUserId());
        order.setCustomerDisplayName(request.getCustomerDisplayName());
        order.setTotalAmount(request.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        order.setSubject(request.getSubject());
        order.setClientIp(clientIp);
        order.setTypeIndex(request.getTypeIndex());
        order.setGoodsType(request.getGoodsType());
        order.setPayMethodType(firstNonBlank(request.getPayMethodType(), config.getDefaultPayMethodType()));
        order.setAttachInfo(request.getAttachInfo());
        order.setReturnUrl(firstNonBlank(
                request.getReturnUrl(),
                PayChannel.PAYMENT_GATEWAY.equals(config.getPayChannel()) ? config.getGatewayReturnUrl() : config.getReturnUrl()
        ));
        order.setQuitUrl(request.getQuitUrl());
        order.setSubExternalId(request.getSubExternalId());
        order.setStatus(PaymentOrderStatus.CREATED);
        return order;
    }

    private void validateProductOrder(CreatePayOrderRequest request) {
        if (request.getProductOrderId() == null) {
            return;
        }
        var productOrder = productOrderRepository.findById(request.getProductOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品订单不存在"));
        if (request.getCustomerUserId() == null || !request.getCustomerUserId().equals(productOrder.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "商品订单不属于当前用户");
        }
        if (!productOrder.getMerchantNo().equals(request.getMerchantNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付商家与商品订单商家不一致");
        }
        if (request.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                .compareTo(productOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付金额与商品订单金额不一致");
        }
    }

    private String generateOrderNo() {
        for (int i = 0; i < 5; i++) {
            String orderNo = "ORDER"
                    + LocalDateTime.now().format(ORDER_TIME_FORMATTER)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (paymentOrderRepository.findByOrderNo(orderNo).isEmpty()) {
                return orderNo;
            }
        }
        throw new IllegalStateException("商户订单号生成失败，请重试");
    }

    private void applyCreateResponse(PaymentOrder order, String rawResponse) {
        // 保存平台原始响应，后续出现对账或排查问题时可以还原现场。
        order.setPlatformCreateResponse(rawResponse);

        JsonNode root = JsonUtils.readTree(rawResponse);
        if (root.path("code").asInt(-1) != 0) {
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            return;
        }

        JsonNode payData = root.path("data").path("data");
        order.setPayUrl(payData.path("payUrl").asText(null));
        order.setEvokeMode(payData.path("evokeMode").asText(null));
        order.setPlatTradeNo(payData.path("platTradeNo").asText(null));
        order.setStatus(PaymentOrderStatus.CREATE_SUCCESS);
    }

    private void applyGatewayCreateResponse(PaymentOrder order, String gatewayRawResponse, String payPageUrl) {
        JsonNode root = JsonUtils.readTree(gatewayRawResponse);
        if (root.path("code").asInt(-1) != 0) {
            order.setPlatformCreateResponse(gatewayRawResponse);
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            return;
        }

        JsonNode data = root.path("data");
        String gatewayOrderNo = data.path("gatewayOrderNo").asText(null);
        order.setPayUrl(payPageUrl);
        order.setEvokeMode("1");
        order.setPlatTradeNo(gatewayOrderNo);
        order.setStatus(PaymentOrderStatus.CREATE_SUCCESS);

        Map<String, Object> compatibleResponse = new TreeMap<>();
        compatibleResponse.put("code", 0);
        compatibleResponse.put("msg", "success");
        compatibleResponse.put("gatewayResponse", JsonUtils.toMap(gatewayRawResponse));
        compatibleResponse.put("data", Map.of("data", Map.of(
                "payUrl", payPageUrl,
                "evokeMode", "1",
                "platTradeNo", gatewayOrderNo == null ? "" : gatewayOrderNo,
                "payForm", data.path("payForm").asText("")
        )));
        order.setPlatformCreateResponse(JsonUtils.toJson(compatibleResponse));
    }

    private static void putIfHasText(Map<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private static void putIfHasTextObject(Map<String, Object> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private static String buildPayPageUrl(HttpServletRequest request, String orderNo) {
        String scheme = firstHeader(request, "X-Forwarded-Proto", request.getScheme());
        String host = firstHeader(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (!StringUtils.hasText(host)) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        return scheme + "://" + host + "/api/mall/pay-orders/" + orderNo + "/pay-page";
    }

    private static String firstHeader(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
