package com.example.mallhome.service;

import com.example.mallhome.client.MallhomePayClient;
import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.domain.CreateRefundRequest;
import com.example.mallhome.domain.PaymentOrderStatus;
import com.example.mallhome.domain.PaymentRefundView;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.entity.PaymentRefund;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.repository.PaymentRefundRepository;
import com.example.mallhome.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentRefundService {

    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private static final DateTimeFormatter REQUEST_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MallhomePayProperties properties;
    private final MallhomePayClient payClient;
    private final PaymentOrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;

    public PaymentRefundService(
            MallhomePayProperties properties,
            MallhomePayClient payClient,
            PaymentOrderRepository orderRepository,
            PaymentRefundRepository refundRepository
    ) {
        this.properties = properties;
        this.payClient = payClient;
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public PaymentRefundView createRefund(String orderNo, CreateRefundRequest request, String createdBy) {
        PaymentOrder order = orderRepository.findWithLockByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));

        validateRefundableOrder(order);

        BigDecimal refundAmount = request.getRefundAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal occupiedAmount = refundRepository
                .findByOrderNoAndStatusIn(orderNo, List.of(PROCESSING, SUCCESS))
                .stream()
                .map(PaymentRefund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundableAmount = order.getTotalAmount().subtract(occupiedAmount);
        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "退款金额超过剩余可退金额 " + refundableAmount.setScale(2, RoundingMode.HALF_UP)
            );
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setRequestNo(generateRequestNo());
        refund.setOrderNo(order.getOrderNo());
        refund.setPlatTradeNo(order.getPlatTradeNo());
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(request.getRefundReason().trim());
        refund.setStatus(PROCESSING);
        refund.setCreatedBy(createdBy);
        refundRepository.saveAndFlush(refund);

        Map<String, String> params = new TreeMap<>();
        params.put("platTradeNo", order.getPlatTradeNo());
        params.put("refundAmount", refundAmount.toPlainString());
        params.put("externalId", properties.getExternalId());
        params.put("refundReason", request.getRefundReason().trim());

        try {
            String rawResponse = payClient.tradeRefund(params);
            applyPlatformResponse(refund, rawResponse);
        } catch (RuntimeException exception) {
            refund.setStatus(FAILED);
            refund.setPlatformResponse(exception.getMessage());
        }

        refundRepository.save(refund);
        return PaymentRefundView.from(refund);
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundView> listRefunds(String orderNo) {
        return refundRepository.findByOrderNoOrderByCreatedAtDesc(orderNo)
                .stream()
                .map(PaymentRefundView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getRefundedAmount(String orderNo) {
        return refundRepository.findByOrderNoAndStatusIn(orderNo, List.of(PROCESSING, SUCCESS))
                .stream()
                .map(PaymentRefund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRefundableOrder(PaymentOrder order) {
        if (!PaymentOrderStatus.SUCCESS.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有交易成功的订单可以退款");
        }
        if (order.getPlatTradeNo() == null || order.getPlatTradeNo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单缺少平台单号，无法退款");
        }
    }

    private void applyPlatformResponse(PaymentRefund refund, String rawResponse) {
        refund.setPlatformResponse(rawResponse);
        JsonNode root = JsonUtils.readTree(rawResponse);
        if (root.path("code").asInt(-1) != 0) {
            refund.setStatus(FAILED);
            return;
        }

        JsonNode data = root.path("data");
        refund.setTradeStatus(data.path("tradeStatus").asText(null));
        refund.setStatus(SUCCESS);
    }

    private String generateRequestNo() {
        return "REFUND"
                + LocalDateTime.now().format(REQUEST_TIME_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
