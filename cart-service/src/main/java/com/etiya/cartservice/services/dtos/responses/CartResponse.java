package com.etiya.cartservice.services.dtos.responses;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private int cartId;
    private int customerId;
    private String status;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;

    public CartResponse() {
    }

    public CartResponse(int cartId, int customerId, String status,
                        List<CartItemResponse> items, BigDecimal totalPrice) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
