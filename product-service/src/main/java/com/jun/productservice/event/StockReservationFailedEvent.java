package com.jun.productservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class StockReservationFailedEvent {
    private final String orderId;
    private final Long productId;
    private final Integer requestedQuantity;
    private final String reason;
    private final LocalDateTime timestamp;

    @JsonCreator
    public StockReservationFailedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("productId") Long productId,
            @JsonProperty("requestedQuantity") Integer requestedQuantity,
            @JsonProperty("reason") String reason) {
        this.orderId = orderId;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}