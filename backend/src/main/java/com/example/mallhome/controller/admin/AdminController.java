package com.example.mallhome.controller.admin;

import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.domain.AdminPageResponse;
import com.example.mallhome.domain.CreateRefundRequest;
import com.example.mallhome.domain.PayConfigView;
import com.example.mallhome.domain.PaymentRefundView;
import com.example.mallhome.domain.PaymentNotifyRecordView;
import com.example.mallhome.domain.PaymentOrderDetailView;
import com.example.mallhome.domain.PaymentOrderView;
import com.example.mallhome.domain.PayRuntimeConfig;
import com.example.mallhome.domain.UpdatePayConfigRequest;
import com.example.mallhome.entity.PaymentNotifyRecord;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.entity.PaymentRefund;
import com.example.mallhome.repository.PaymentNotifyRecordRepository;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.repository.PaymentRefundRepository;
import com.example.mallhome.service.AdminAuthService;
import com.example.mallhome.service.PaymentRefundService;
import com.example.mallhome.service.PayRuntimeConfigService;
import jakarta.validation.Valid;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentNotifyRecordRepository notifyRecordRepository;
    private final MallhomePayProperties payProperties;
    private final PaymentRefundService refundService;
    private final PaymentRefundRepository refundRepository;
    private final AdminAuthService authService;
    private final PayRuntimeConfigService runtimeConfigService;

    public AdminController(
            PaymentOrderRepository paymentOrderRepository,
            PaymentNotifyRecordRepository notifyRecordRepository,
            MallhomePayProperties payProperties,
            PaymentRefundService refundService,
            PaymentRefundRepository refundRepository,
            AdminAuthService authService,
            PayRuntimeConfigService runtimeConfigService
    ) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.notifyRecordRepository = notifyRecordRepository;
        this.payProperties = payProperties;
        this.refundService = refundService;
        this.refundRepository = refundRepository;
        this.authService = authService;
        this.runtimeConfigService = runtimeConfigService;
    }

    @GetMapping("/pay-orders")
    public AdminPageResponse<PaymentOrderView> listPayOrders(
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
        Specification<PaymentOrder> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
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
    public PaymentOrderDetailView getPayOrder(@PathVariable String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .map(PaymentOrderDetailView::fromOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    @GetMapping("/pay-orders/{orderNo}/refunds")
    public List<PaymentRefundView> listRefunds(@PathVariable String orderNo) {
        return refundService.listRefunds(orderNo);
    }

    @PostMapping("/pay-orders/{orderNo}/refunds")
    public PaymentRefundView createRefund(
            @PathVariable String orderNo,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.substring("Bearer ".length());
        String username = authService.requireUser(token).getUsername();
        return refundService.createRefund(orderNo, request, username);
    }

    @GetMapping("/pay-refunds")
    public AdminPageResponse<PaymentRefundView> listAllRefunds(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Specification<PaymentRefund> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(orderNo)) {
                predicates.add(builder.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return new AdminPageResponse<>(refundRepository
                .findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PaymentRefundView::from));
    }

    @GetMapping("/pay-config")
    public PayConfigView getPayConfig() {
        PayRuntimeConfig config = runtimeConfigService.getMaskedConfig();
        return new PayConfigView(
                config.getPayChannel(),
                config.getHost(),
                config.getExternalId(),
                config.getNotifyUrl(),
                config.getReturnUrl(),
                config.getDefaultPayMethodType(),
                config.getGatewayHost(),
                config.getGatewayAppId(),
                config.getGatewayAppSecret(),
                config.getGatewayReturnUrl(),
                config.getGatewayBusinessNotifyUrl()
        );
    }

    @PutMapping("/pay-config")
    public void savePayConfig(@RequestBody UpdatePayConfigRequest request) {
        runtimeConfigService.updateConfig(request);
    }

    @GetMapping("/pay-notifies")
    public AdminPageResponse<PaymentNotifyRecordView> listPayNotifies(
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
        Specification<PaymentNotifyRecord> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
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
}
