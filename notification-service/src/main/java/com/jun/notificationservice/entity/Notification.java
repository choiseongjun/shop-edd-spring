package com.jun.notificationservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@org.hibernate.annotations.Check(constraints = "type IN ('ORDER_CREATED', 'ORDER_PLACED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED', 'ORDER_DELIVERED', 'ORDER_CANCELLED', 'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_REFUNDED', 'STOCK_SHORTAGE', 'FLASH_SALE_STARTED', 'FLASH_SALE_REMINDER', 'FLASH_SALE_ENDED', 'SYSTEM_MAINTENANCE', 'ACCOUNT_SECURITY', 'PROMOTIONAL')")
public class Notification {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "sms_sent")
    private Boolean smsSent = false;

    @Column(name = "push_sent")
    private Boolean pushSent = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Notification() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        if (isRead && readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

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

    public enum NotificationType {
        ORDER_CREATED, ORDER_PLACED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED,
        PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_REFUNDED,
        STOCK_SHORTAGE,
        FLASH_SALE_STARTED, FLASH_SALE_REMINDER, FLASH_SALE_ENDED,
        SYSTEM_MAINTENANCE, ACCOUNT_SECURITY, PROMOTIONAL
    }

    public enum NotificationChannel {
        IN_APP, EMAIL, SMS, PUSH, WEBSOCKET
    }
}