package com.jun.paymentservice.dto;

import com.jun.paymentservice.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDto {
    private Long id;
    private String paymentId;
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String failureReason;
    private String refundReason;
    private BigDecimal refundedAmount;
    private Boolean flashSalePayment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;

    public PaymentDto() {}

    public PaymentDto(Payment payment) {
        this.id = payment.getId();
        this.paymentId = payment.getPaymentId();
        this.orderId = payment.getOrderId();
        this.userId = payment.getUserId();
        this.amount = payment.getAmount();
        this.paymentMethod = payment.getPaymentMethod().name();
        this.status = payment.getStatus().name();
        this.transactionId = payment.getTransactionId();
        this.failureReason = payment.getFailureReason();
        this.refundReason = payment.getRefundReason();
        this.refundedAmount = payment.getRefundedAmount();
        this.flashSalePayment = payment.getFlashSalePayment();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
        this.processedAt = payment.getProcessedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public Boolean getFlashSalePayment() { return flashSalePayment; }
    public void setFlashSalePayment(Boolean flashSalePayment) { this.flashSalePayment = flashSalePayment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}