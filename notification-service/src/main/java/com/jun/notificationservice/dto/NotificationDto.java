package com.jun.notificationservice.dto;

import com.jun.notificationservice.entity.Notification;

import java.time.LocalDateTime;

public class NotificationDto {
    private String id;
    private Long userId;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Notification.NotificationChannel channel;
    private Boolean isRead;
    private String orderId;
    private String paymentId;
    private Boolean emailSent;
    private Boolean smsSent;
    private Boolean pushSent;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public NotificationDto() {}

    public NotificationDto(Notification notification) {
        this.id = notification.getId();
        this.userId = notification.getUserId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.type = notification.getType();
        this.channel = notification.getChannel();
        this.isRead = notification.getIsRead();
        this.orderId = notification.getOrderId();
        this.paymentId = notification.getPaymentId();
        this.emailSent = notification.getEmailSent();
        this.smsSent = notification.getSmsSent();
        this.pushSent = notification.getPushSent();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public Boolean getEmailSent() { return emailSent; }
    public void setEmailSent(Boolean emailSent) { this.emailSent = emailSent; }

    public Boolean getSmsSent() { return smsSent; }
    public void setSmsSent(Boolean smsSent) { this.smsSent = smsSent; }

    public Boolean getPushSent() { return pushSent; }
    public void setPushSent(Boolean pushSent) { this.pushSent = pushSent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
