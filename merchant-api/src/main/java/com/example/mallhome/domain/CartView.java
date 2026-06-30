package com.example.mallhome.domain;

import java.util.List;

public class CartView {

    private List<CartItemView> items;
    private int totalQuantity;

    public CartView() {
    }

    public CartView(List<CartItemView> items) {
        this.items = items;
        this.totalQuantity = items.stream().mapToInt(CartItemView::getQuantity).sum();
    }

    public List<CartItemView> getItems() {
        return items;
    }

    public void setItems(List<CartItemView> items) {
        this.items = items;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}
