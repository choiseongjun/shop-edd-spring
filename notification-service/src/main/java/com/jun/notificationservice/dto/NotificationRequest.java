package com.jun.notificationservice.dto;

import com.jun.notificationservice.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificationRequest {
    @NotNull
    private Long userId;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
    
    @NotNull
    private Notification.NotificationType type;
    
    @NotNull
    private Notification.NotificationChannel channel;
    
    private String orderId;
    private String paymentId;

    public NotificationRequest() {}

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Notification.NotificationType getType() { return type; }
    public void setType(Notification.NotificationType type) { this.type = type; }

    public Notification.NotificationChannel getChannel() { return channel; }
    public void setChannel(Notification.NotificationChannel channel) { this.channel = channel; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
}

