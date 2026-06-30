package com.example.mallhome.controller.merchant;

import com.example.mallhome.domain.AdminPageResponse;
import com.example.mallhome.domain.CreateRefundRequest;
import com.example.mallhome.domain.PaymentNotifyRecordView;
import com.example.mallhome.domain.PaymentOrderDetailView;
import com.example.mallhome.domain.PaymentOrderView;
import com.example.mallhome.domain.PaymentRefundView;
import com.example.mallhome.domain.ProductOrderView;
import com.example.mallhome.service.MerchantService;
import com.example.mallhome.service.ProductOrderService;
import com.example.mallhome.service.PayRuntimeConfigService;
import com.example.mallhome.domain.PayRuntimeConfig;
import com.example.mallhome.domain.UpdatePayConfigRequest;
import com.example.mallhome.entity.PaymentNotifyRecord;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.entity.PaymentRefund;
import com.example.mallhome.repository.PaymentNotifyRecordRepository;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.repository.PaymentRefundRepository;
import com.example.mallhome.service.PaymentRefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final PayRuntimeConfigService payRuntimeConfigService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentNotifyRecordRepository notifyRecordRepository;
    private final PaymentRefundService refundService;
    private final PaymentRefundRepository refundRepository;
    private final ProductOrderService productOrderService;

    public MerchantController(
            MerchantService merchantService,
            PayRuntimeConfigService payRuntimeConfigService,
            PaymentOrderRepository paymentOrderRepository,
            PaymentNotifyRecordRepository notifyRecordRepository,
            PaymentRefundService refundService,
            PaymentRefundRepository refundRepository,
            ProductOrderService productOrderService
    ) {
        this.merchantService = merchantService;
        this.payRuntimeConfigService = payRuntimeConfigService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.notifyRecordRepository = notifyRecordRepository;
        this.refundService = refundService;
        this.refundRepository = refundRepository;
        this.productOrderService = productOrderService;
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> payload) {
        return merchantService.register(payload);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> payload) {
        return merchantService.login(payload);
    }

    @PostMapping("/auth/logout")
    public void logout(HttpServletRequest request) {
        merchantService.logout(extractToken(request));
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return merchantService.merchantView(currentMerchant(request));
    }

    @GetMapping("/products")
    public Map<String, Object> products(HttpServletRequest request, @RequestParam Map<String, String> params) {
        return merchantService.listProducts(currentMerchant(request), params);
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return merchantService.listCategories();
    }

    @GetMapping("/pay-config")
    public PayRuntimeConfig payConfig(HttpServletRequest request) {
        Map<String, Object> merchant = currentMerchant(request);
        return payRuntimeConfigService.getMaskedConfigForMerchant(String.valueOf(merchant.get("merchant_no")));
    }

    @PutMapping("/pay-config")
    public void savePayConfig(HttpServletRequest request, @RequestBody UpdatePayConfigRequest payload) {
        Map<String, Object> merchant = currentMerchant(request);
        payRuntimeConfigService.updateMerchantConfig(String.valueOf(merchant.get("merchant_no")), payload);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> product(HttpServletRequest request, @PathVariable Long id) {
        return merchantService.getProduct(currentMerchant(request), id);
    }

    @PostMapping("/products")
    public Map<String, Object> createProduct(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        return merchantService.createProduct(currentMerchant(request), payload);
    }

    @PutMapping("/products/{id}")
    public void updateProduct(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> payload) {
        merchantService.updateProduct(currentMerchant(request), id, payload);
    }

    @GetMapping("/pay-orders")
    public AdminPageResponse<PaymentOrderView> listPayOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platTradeNo,
            @RequestParam(required = false) String thirdOutTradeNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String merchantNo = merchantNo(request);
        Specification<PaymentOrder> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("externalId"), merchantNo));
            if (StringUtils.hasText(orderNo)) {
                predicates.add(builder.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            if (StringUtils.hasText(platTradeNo)) {
                predicates.add(builder.like(root.get("platTradeNo"), "%" + platTradeNo.trim() + "%"));
            }
            if (StringUtils.hasText(thirdOutTradeNo)) {
                predicates.add(builder.like(root.get("thirdOutTradeNo"), "%" + thirdOutTradeNo.trim() + "%"));
            }
            if (createdAtStart != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart));
            }
            if (createdAtEnd != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return new AdminPageResponse<>(paymentOrderRepository
                .findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PaymentOrderView::from));
    }

    @GetMapping("/pay-orders/{orderNo}")
    public PaymentOrderDetailView getPayOrder(HttpServletRequest request, @PathVariable String orderNo) {
        String merchantNo = merchantNo(request);
        PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                .filter(item -> merchantNo.equals(item.getExternalId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
        return PaymentOrderDetailView.fromOrder(order);
    }

    @GetMapping("/pay-orders/{orderNo}/refunds")
    public List<PaymentRefundView> listRefunds(HttpServletRequest request, @PathVariable String orderNo) {
        ensureOrderBelongsToMerchant(request, orderNo);
        return refundService.listRefunds(orderNo);
    }

    @PostMapping("/pay-orders/{orderNo}/refunds")
    public PaymentRefundView createRefund(
            HttpServletRequest request,
            @PathVariable String orderNo,
            @Valid @RequestBody CreateRefundRequest payload
    ) {
        Map<String, Object> merchant = currentMerchant(request);
        ensureOrderBelongsToMerchant(merchant, orderNo);
        return refundService.createRefund(orderNo, payload, String.valueOf(merchant.get("username")));
    }

    @GetMapping("/pay-refunds")
    public AdminPageResponse<PaymentRefundView> listAllRefunds(
            HttpServletRequest request,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String merchantNo = merchantNo(request);
        Specification<PaymentRefund> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            var orderSubquery = query.subquery(String.class);
            var orderRoot = orderSubquery.from(PaymentOrder.class);
            orderSubquery.select(orderRoot.get("orderNo"))
                    .where(builder.equal(orderRoot.get("externalId"), merchantNo));
            predicates.add(root.get("orderNo").in(orderSubquery));
            if (StringUtils.hasText(orderNo)) {
                predicates.add(builder.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            if (createdAtStart != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart));
            }
            if (createdAtEnd != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        return new AdminPageResponse<>(refundRepository
                .findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PaymentRefundView::from));
    }

    @GetMapping("/pay-notifies")
    public AdminPageResponse<PaymentNotifyRecordView> listPayNotifies(
            HttpServletRequest request,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String tradeStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime updatedAtStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime updatedAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String merchantNo = merchantNo(request);
        Specification<PaymentNotifyRecord> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("externalId"), merchantNo));
            if (StringUtils.hasText(orderNo)) {
                predicates.add(builder.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
            }
            if (verified != null) {
                predicates.add(builder.equal(root.get("verified"), verified));
            }
            if (StringUtils.hasText(tradeStatus)) {
                predicates.add(builder.equal(root.get("tradeStatus"), tradeStatus.trim()));
            }
            if (updatedAtStart != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("updatedAt"), updatedAtStart));
            }
            if (updatedAtEnd != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("updatedAt"), updatedAtEnd));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return new AdminPageResponse<>(notifyRecordRepository
                .findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PaymentNotifyRecordView::from));
    }

    private Map<String, Object> currentMerchant(HttpServletRequest request) {
        return merchantService.requireMerchant(extractToken(request));
    }

    private String merchantNo(HttpServletRequest request) {
        return String.valueOf(currentMerchant(request).get("merchant_no"));
    }

    private void ensureOrderBelongsToMerchant(HttpServletRequest request, String orderNo) {
        ensureOrderBelongsToMerchant(currentMerchant(request), orderNo);
    }

    private void ensureOrderBelongsToMerchant(Map<String, Object> merchant, String orderNo) {
        String merchantNo = String.valueOf(merchant.get("merchant_no"));
        paymentOrderRepository.findByOrderNo(orderNo)
                .filter(item -> merchantNo.equals(item.getExternalId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    @GetMapping("/product-orders")
    public AdminPageResponse<ProductOrderView> listProductOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        String merchantNo = merchantNo(request);
        return new AdminPageResponse<>(productOrderService.searchOrders(merchantNo, orderNo, status, keyword, createdAtStart, createdAtEnd, page, size));
    }

    @GetMapping("/product-orders/{orderNo}")
    public ProductOrderView getProductOrder(HttpServletRequest request, @PathVariable String orderNo) {
        return productOrderService.getMerchantOrder(merchantNo(request), orderNo);
    }

    @PutMapping("/product-orders/{orderNo}/status")
    public ProductOrderView updateProductOrderStatus(
            HttpServletRequest request,
            @PathVariable String orderNo,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        return productOrderService.updateMerchantStatus(merchantNo(request), orderNo, newStatus);
    }
}
