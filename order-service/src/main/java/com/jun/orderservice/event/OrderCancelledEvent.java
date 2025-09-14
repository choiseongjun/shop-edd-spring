package com.jun.orderservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCancelledEvent {
    private final String orderId;
    private final Long userId;
    private final String reason;
    private final List<OrderItemInfo> orderItems;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;

    @JsonCreator
    public OrderCancelledEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("reason") String reason,
            @JsonProperty("orderItems") List<OrderItemInfo> orderItems,
            @JsonProperty("totalAmount") BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public String getReason() { return reason; }
    public List<OrderItemInfo> getOrderItems() { return orderItems; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public static class OrderItemInfo {
        private final Long productId;
        private final Integer quantity;

        @JsonCreator
        public OrderItemInfo(
                @JsonProperty("productId") Long productId,
                @JsonProperty("quantity") Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
    }
}