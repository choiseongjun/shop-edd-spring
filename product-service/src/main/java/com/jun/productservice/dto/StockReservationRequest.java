package com.jun.productservice.dto;

public class StockReservationRequest {
    private Long productId;
    private Integer quantity;
    private Long userId;
    private String orderId;

    public StockReservationRequest() {}

    public StockReservationRequest(Long productId, Integer quantity, Long userId, String orderId) {
        this.productId = productId;
        this.quantity = quantity;
        this.userId = userId;
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}