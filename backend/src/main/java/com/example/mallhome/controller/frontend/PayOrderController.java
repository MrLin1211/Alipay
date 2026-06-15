package com.example.mallhome.controller.frontend;

import com.example.mallhome.domain.CreatePayOrderRequest;
import com.example.mallhome.domain.CreatePayOrderResponse;
import com.example.mallhome.domain.PaymentOrderView;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.service.PayOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/pay-orders")
public class PayOrderController {

    private final PayOrderService payOrderService;
    private final PaymentOrderRepository paymentOrderRepository;

    public PayOrderController(PayOrderService payOrderService, PaymentOrderRepository paymentOrderRepository) {
        this.payOrderService = payOrderService;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @PostMapping
    public CreatePayOrderResponse createPayOrder(@Valid @RequestBody CreatePayOrderRequest request, HttpServletRequest servletRequest) {
        return payOrderService.createPayOrder(request, servletRequest);
    }

    @GetMapping("/{orderNo}")
    public PaymentOrderView getPayOrder(@PathVariable String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .map(PaymentOrderView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }
}
