package com.etiya.cartservice.services.dtos.responses;

import java.math.BigDecimal;

public class CartItemResponse {

    private int productId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public CartItemResponse() {
    }

    public CartItemResponse(int productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
