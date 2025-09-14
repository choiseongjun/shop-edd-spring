package com.jun.orderservice.service;

import com.jun.orderservice.event.OrderCreatedEvent;
import com.jun.orderservice.event.OrderCancelledEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish OrderCreatedEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            kafkaTemplate.send(ORDER_CANCELLED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish OrderCancelledEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}