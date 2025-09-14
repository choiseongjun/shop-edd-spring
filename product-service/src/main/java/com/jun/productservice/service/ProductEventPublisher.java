package com.jun.productservice.service;

import com.jun.productservice.event.StockReservedEvent;
import com.jun.productservice.event.StockReservationFailedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductEventPublisher {

    private static final String STOCK_RESERVED_TOPIC = "stock-reserved";
    private static final String STOCK_RESERVATION_FAILED_TOPIC = "stock-reservation-failed";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStockReserved(StockReservedEvent event) {
        try {
            kafkaTemplate.send(STOCK_RESERVED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish StockReservedEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    public void publishStockReservationFailed(StockReservationFailedEvent event) {
        try {
            kafkaTemplate.send(STOCK_RESERVATION_FAILED_TOPIC, event.getOrderId(), event);
        } catch (Exception e) {
            System.err.println("Failed to publish StockReservationFailedEvent: " + e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}