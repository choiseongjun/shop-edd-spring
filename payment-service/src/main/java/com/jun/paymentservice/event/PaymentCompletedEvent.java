package com.jun.paymentservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEvent {
    private final String paymentId;
    private final String orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final String transactionId;
    private final LocalDateTime timestamp;

    @JsonCreator
    public PaymentCompletedEvent(
            @JsonProperty("paymentId") String paymentId,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("transactionId") String transactionId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}