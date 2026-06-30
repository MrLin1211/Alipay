package com.example.mallhome.controller.frontend;

import com.example.mallhome.domain.CreatePayOrderRequest;
import com.example.mallhome.domain.CreatePayOrderResponse;
import com.example.mallhome.domain.PaymentOrderView;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.service.CustomerAuthService;
import com.example.mallhome.service.PayOrderService;
import com.example.mallhome.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/mall/pay-orders")
public class PayOrderController {

    private final PayOrderService payOrderService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final CustomerAuthService customerAuthService;

    public PayOrderController(
            PayOrderService payOrderService,
            PaymentOrderRepository paymentOrderRepository,
            CustomerAuthService customerAuthService
    ) {
        this.payOrderService = payOrderService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.customerAuthService = customerAuthService;
    }

    @PostMapping
    public CreatePayOrderResponse createPayOrder(@Valid @RequestBody CreatePayOrderRequest request, HttpServletRequest servletRequest) {
        var customer = customerAuthService.requireUser(extractToken(servletRequest));
        request.setCustomerUserId(Long.valueOf(String.valueOf(customer.get("id"))));
        request.setCustomerDisplayName(String.valueOf(customer.get("display_name")));
        return payOrderService.createPayOrder(request, servletRequest);
    }

    @GetMapping("/{orderNo}")
    public PaymentOrderView getPayOrder(@PathVariable String orderNo, HttpServletRequest servletRequest) {
        var customer = customerAuthService.requireUser(extractToken(servletRequest));
        Long customerUserId = Long.valueOf(String.valueOf(customer.get("id")));
        return paymentOrderRepository.findByOrderNo(orderNo)
                .filter(order -> customerUserId.equals(order.getCustomerUserId()))
                .map(PaymentOrderView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    @GetMapping(value = "/{orderNo}/pay-page", produces = MediaType.TEXT_HTML_VALUE)
    public String getPayPage(@PathVariable String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .map(order -> {
                    JsonNode root = JsonUtils.readTree(order.getPlatformCreateResponse());
                    String payForm = root.path("data").path("data").path("payForm").asText("");
                    if (payForm.isBlank()) {
                        return "<!doctype html><html lang=\"zh-CN\"><body>支付表单不存在</body></html>";
                    }
                    return payForm;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return "";
    }
}
