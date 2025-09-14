package com.jun.productservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class StockReservedEvent {
    private final String orderId;
    private final Long productId;
    private final Integer quantity;
    private final String productName;
    private final LocalDateTime timestamp;

    @JsonCreator
    public StockReservedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("productId") Long productId,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("productName") String productName) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.productName = productName;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getProductName() { return productName; }
    public LocalDateTime getTimestamp() { return timestamp; }
}