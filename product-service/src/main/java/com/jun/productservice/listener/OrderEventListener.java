package com.jun.productservice.listener;

import com.jun.productservice.event.StockReservedEvent;
import com.jun.productservice.event.StockReservationFailedEvent;
import com.jun.productservice.service.ProductEventPublisher;
import com.jun.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderEventListener {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductEventPublisher eventPublisher;

    @KafkaListener(topics = "order-created", groupId = "product-service-group")
    public void handleOrderCreated(Map<String, Object> orderData) {
        try {
            String orderId = (String) orderData.get("orderId");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> orderItems = (java.util.List<Map<String, Object>>) orderData.get("orderItems");

            for (Map<String, Object> item : orderItems) {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();
                String productName = (String) item.get("productName");

                try {
                    boolean reserved = productService.reserveStock(productId, quantity, orderId);

                    if (reserved) {
                        eventPublisher.publishStockReserved(new StockReservedEvent(
                            orderId, productId, quantity, productName
                        ));
                    } else {
                        eventPublisher.publishStockReservationFailed(new StockReservationFailedEvent(
                            orderId, productId, quantity, "Insufficient stock"
                        ));
                    }
                } catch (Exception e) {
                    eventPublisher.publishStockReservationFailed(new StockReservationFailedEvent(
                        orderId, productId, quantity, e.getMessage()
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle order created event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "product-service-group")
    public void handleOrderCancelled(Map<String, Object> orderData) {
        try {
            String orderId = (String) orderData.get("orderId");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> orderItems = (java.util.List<Map<String, Object>>) orderData.get("orderItems");

            for (Map<String, Object> item : orderItems) {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();

                try {
                    productService.releaseReservedStock(productId, quantity, orderId);
                } catch (Exception e) {
                    System.err.println("Failed to release reserved stock for order: " + orderId + ", product: " + productId);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle order cancelled event: " + e.getMessage());
        }
    }
}