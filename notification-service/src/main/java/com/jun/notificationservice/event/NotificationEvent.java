package com.jun.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class NotificationEvent {
    private final String notificationId;
    private final Long userId;
    private final String type;
    private final String title;
    private final String message;
    private final String relatedId;
    private final LocalDateTime timestamp;

    @JsonCreator
    public NotificationEvent(
            @JsonProperty("notificationId") String notificationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("message") String message,
            @JsonProperty("relatedId") String relatedId) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedId = relatedId;
        this.timestamp = LocalDateTime.now();
    }

    public String getNotificationId() { return notificationId; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRelatedId() { return relatedId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}