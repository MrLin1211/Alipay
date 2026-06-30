package com.example.mallhome.controller.frontend;

import com.example.mallhome.domain.ProductOrderView;
import com.example.mallhome.service.AddressService;
import com.example.mallhome.service.CustomerAuthService;
import com.example.mallhome.service.ProductOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mall/product-orders")
public class ProductOrderController {

    private final ProductOrderService productOrderService;
    private final CustomerAuthService customerAuthService;
    private final AddressService addressService;

    public ProductOrderController(ProductOrderService productOrderService, CustomerAuthService customerAuthService,
                                  AddressService addressService) {
        this.productOrderService = productOrderService;
        this.customerAuthService = customerAuthService;
        this.addressService = addressService;
    }

    @GetMapping
    public List<ProductOrderView> getUserOrders(HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        return productOrderService.getUserOrders(userId);
    }

    @GetMapping("/{orderNo}")
    public ProductOrderView getOrder(@PathVariable String orderNo, HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        return productOrderService.getUserOrder(userId, orderNo);
    }

    @PostMapping
    public ProductOrderView createOrder(@RequestBody Map<String, Object> payload, HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        Map<String, Object> customer = customerAuthService.requireUser(extractToken(servletRequest));
        String userPhone = (String) customer.get("phone");
        String userDisplayName = (String) customer.get("display_name");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        Long addressId = toLong(payload.get("addressId"));
        if (addressId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择收货地址");
        }
        Map<String, Object> address = addressService.getUserAddress(userId, addressId);
        String shippingAddress = formatShippingAddress(address);
        return productOrderService.createOrder(
                userId, userPhone, userDisplayName,
                (String) payload.get("merchantNo"),
                (String) payload.get("merchantName"),
                toBigDecimal(payload.get("totalAmount")),
                toBigDecimal(payload.get("discountAmount")),
                items,
                shippingAddress,
                String.valueOf(address.get("receiverName")),
                String.valueOf(address.get("phone")),
                (String) payload.get("remark")
        );
    }

    private String formatShippingAddress(Map<String, Object> address) {
        return String.join(" ",
                String.valueOf(address.get("province")),
                String.valueOf(address.get("city")),
                String.valueOf(address.get("district")),
                String.valueOf(address.get("detailAddress"))
        ).replaceAll("\\s+", " ").trim();
    }

    private Long requireUserId(HttpServletRequest request) {
        Map<String, Object> customer = customerAuthService.requireUser(extractToken(request));
        return Long.valueOf(String.valueOf(customer.get("id")));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return "";
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value instanceof java.math.BigDecimal bd) return bd;
        if (value instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s) {
            try { return new java.math.BigDecimal(s); } catch (NumberFormatException e) { return java.math.BigDecimal.ZERO; }
        }
        return java.math.BigDecimal.ZERO;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
