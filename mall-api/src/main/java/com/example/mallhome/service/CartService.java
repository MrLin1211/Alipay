package com.example.mallhome.service;

import com.example.mallhome.domain.CartItemView;
import com.example.mallhome.domain.CartUpsertRequest;
import com.example.mallhome.domain.CartView;
import com.example.mallhome.entity.CartItem;
import com.example.mallhome.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final int MAX_QUANTITY = 999;

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return new CartView(items.stream().map(CartItemView::from).toList());
    }

    @Transactional
    public CartItemView upsertItem(Long userId, CartUpsertRequest request) {
        if (request.getQuantity() <= 0) {
            cartItemRepository.deleteByUserIdAndProductIdAndSkuId(userId, request.getProductId(), request.getSkuId());
            return null;
        }

        int qty = Math.min(request.getQuantity(), MAX_QUANTITY);
        CartItem item = cartItemRepository.findByUserIdAndProductIdAndSkuId(userId, request.getProductId(), request.getSkuId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUserId(userId);
                    newItem.setProductId(request.getProductId());
                    newItem.setSkuId(request.getSkuId());
                    return newItem;
                });

        item.setQuantity(qty);
        if (request.getProductCode() != null) item.setProductCode(request.getProductCode());
        if (request.getSkuCode() != null) item.setSkuCode(request.getSkuCode());
        if (request.getSkuName() != null) item.setSkuName(request.getSkuName());
        if (request.getProductName() != null) item.setProductName(request.getProductName());
        if (request.getProductImage() != null) item.setProductImage(request.getProductImage());
        if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
        else if (item.getUnitPrice() == null) item.setUnitPrice(BigDecimal.ZERO);

        cartItemRepository.save(item);
        return CartItemView.from(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Transactional
    public CartView mergeCart(Long userId, List<Map<String, Object>> incomingItems) {
        for (Map<String, Object> incoming : incomingItems) {
            Long productId = toLong(incoming.get("productId"));
            Long skuId = toLong(incoming.get("skuId"));
            if (productId == null) continue;

            int incomingQty = toInt(incoming.get("quantity"), 1);
            if (incomingQty <= 0) continue;

            cartItemRepository.findByUserIdAndProductIdAndSkuId(userId, productId, skuId)
                    .ifPresentOrElse(existing -> {
                        int newQty = Math.min(existing.getQuantity() + incomingQty, MAX_QUANTITY);
                        existing.setQuantity(newQty);
                        applySnapshot(existing, incoming);
                        cartItemRepository.save(existing);
                    }, () -> {
                        CartItem newItem = new CartItem();
                        newItem.setUserId(userId);
                        newItem.setProductId(productId);
                        newItem.setSkuId(skuId);
                        newItem.setQuantity(Math.min(incomingQty, MAX_QUANTITY));
                        applySnapshot(newItem, incoming);
                        if (newItem.getUnitPrice() == null) newItem.setUnitPrice(BigDecimal.ZERO);
                        cartItemRepository.save(newItem);
                    });
        }
        return getCart(userId);
    }

    private void applySnapshot(CartItem item, Map<String, Object> data) {
        if (data.get("productCode") != null) item.setProductCode((String) data.get("productCode"));
        if (data.get("skuCode") != null) item.setSkuCode((String) data.get("skuCode"));
        if (data.get("skuName") != null) item.setSkuName((String) data.get("skuName"));
        if (data.get("productName") != null) item.setProductName((String) data.get("productName"));
        if (data.get("productImage") != null) item.setProductImage((String) data.get("productImage"));
        if (data.get("unitPrice") != null) {
            item.setUnitPrice(new BigDecimal(data.get("unitPrice").toString()));
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
}
