package com.jun.paymentservice.service;

import com.jun.paymentservice.event.PaymentCompletedEvent;
import com.jun.paymentservice.event.PaymentFailedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish PaymentCompletedEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish PaymentFailedEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}