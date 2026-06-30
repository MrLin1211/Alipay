package com.example.mallhome.controller.frontend;

import com.example.mallhome.domain.CartItemView;
import com.example.mallhome.domain.CartUpsertRequest;
import com.example.mallhome.domain.CartView;
import com.example.mallhome.service.CartService;
import com.example.mallhome.service.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mall/cart")
public class CartController {

    private final CartService cartService;
    private final CustomerAuthService customerAuthService;

    public CartController(CartService cartService, CustomerAuthService customerAuthService) {
        this.cartService = cartService;
        this.customerAuthService = customerAuthService;
    }

    @GetMapping
    public CartView getCart(HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        return cartService.getCart(userId);
    }

    @PostMapping("/items")
    public ResponseEntity<?> upsertItem(@Valid @RequestBody CartUpsertRequest request, HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        CartItemView result = cartService.upsertItem(userId, request);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public CartView mergeCart(@RequestBody Map<String, Object> payload, HttpServletRequest servletRequest) {
        Long userId = requireUserId(servletRequest);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        if (items == null || items.isEmpty()) {
            return cartService.getCart(userId);
        }
        return cartService.mergeCart(userId, items);
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
}
