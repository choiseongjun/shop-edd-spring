package com.jun.orderservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent {
    private final String orderId;
    private final Long userId;
    private final String shippingAddress;
    private final String paymentMethod;
    private final BigDecimal totalAmount;
    private final List<OrderItemInfo> orderItems;
    private final boolean isFlashSaleOrder;
    private final LocalDateTime timestamp;

    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("shippingAddress") String shippingAddress,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("orderItems") List<OrderItemInfo> orderItems,
            @JsonProperty("isFlashSaleOrder") boolean isFlashSaleOrder) {
        this.orderId = orderId;
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.orderItems = orderItems;
        this.isFlashSaleOrder = isFlashSaleOrder;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public String getShippingAddress() { return shippingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItemInfo> getOrderItems() { return orderItems; }
    public boolean isFlashSaleOrder() { return isFlashSaleOrder; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public static class OrderItemInfo {
        private final Long productId;
        private final Integer quantity;
        private final String productName;
        private final BigDecimal unitPrice;
        private final boolean isFlashSaleItem;

        @JsonCreator
        public OrderItemInfo(
                @JsonProperty("productId") Long productId,
                @JsonProperty("quantity") Integer quantity,
                @JsonProperty("productName") String productName,
                @JsonProperty("unitPrice") BigDecimal unitPrice,
                @JsonProperty("isFlashSaleItem") boolean isFlashSaleItem) {
            this.productId = productId;
            this.quantity = quantity;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.isFlashSaleItem = isFlashSaleItem;
        }

        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
        public String getProductName() { return productName; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public boolean isFlashSaleItem() { return isFlashSaleItem; }
    }
}