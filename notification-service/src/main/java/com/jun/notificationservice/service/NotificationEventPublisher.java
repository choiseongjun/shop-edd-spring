package com.jun.notificationservice.service;

import com.jun.notificationservice.event.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventPublisher {

    private static final String NOTIFICATION_TOPIC = "user-notifications";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishNotification(NotificationEvent event) {
        try {
            kafkaTemplate.send(NOTIFICATION_TOPIC, event.getUserId().toString(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish NotificationEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}