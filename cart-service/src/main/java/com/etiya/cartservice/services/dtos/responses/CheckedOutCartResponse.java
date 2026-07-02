package com.etiya.cartservice.services.dtos.responses;

import java.math.BigDecimal;

public class CheckedOutCartResponse {

    private int cartId;
    private int customerId;
    private String status;
    private BigDecimal totalPrice;

    public CheckedOutCartResponse() {
    }

    public CheckedOutCartResponse(int cartId, int customerId, String status, BigDecimal totalPrice) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.status = status;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
