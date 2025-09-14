package com.jun.orderservice.listener;

import com.jun.orderservice.service.OrderService;
import com.jun.orderservice.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentEventListener {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payment-completed", groupId = "order-service-group")
    public void handlePaymentCompleted(Map<String, Object> paymentData) {
        try {
            String orderId = (String) paymentData.get("orderId");
            Long userId = ((Number) paymentData.get("userId")).longValue();

            boolean updated = orderService.updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED.name(), userId);
            if (updated) {
                System.out.println("Order confirmed successfully: " + orderId);
            } else {
                System.err.println("Failed to confirm order: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle payment completed event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(Map<String, Object> paymentData) {
        try {
            String orderId = (String) paymentData.get("orderId");
            Long userId = ((Number) paymentData.get("userId")).longValue();
            String failureReason = (String) paymentData.get("failureReason");

            boolean updated = orderService.updateOrderStatus(orderId, Order.OrderStatus.CANCELLED.name(), userId);
            if (updated) {
                System.out.println("Order cancelled due to payment failure: " + orderId + ", Reason: " + failureReason);
            } else {
                System.err.println("Failed to cancel order: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle payment failed event: " + e.getMessage());
        }
    }
}