package com.jun.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cardCvv;

    private String bankAccount;
    private String routingNumber;

    private String walletId;
    private String walletToken;

    private Boolean flashSalePayment = false;

    private Long userId;

    public PaymentRequest() {}

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCardExpiryMonth() { return cardExpiryMonth; }
    public void setCardExpiryMonth(String cardExpiryMonth) { this.cardExpiryMonth = cardExpiryMonth; }

    public String getCardExpiryYear() { return cardExpiryYear; }
    public void setCardExpiryYear(String cardExpiryYear) { this.cardExpiryYear = cardExpiryYear; }

    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getRoutingNumber() { return routingNumber; }
    public void setRoutingNumber(String routingNumber) { this.routingNumber = routingNumber; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getWalletToken() { return walletToken; }
    public void setWalletToken(String walletToken) { this.walletToken = walletToken; }

    public Boolean getFlashSalePayment() { return flashSalePayment; }
    public void setFlashSalePayment(Boolean flashSalePayment) { this.flashSalePayment = flashSalePayment; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}