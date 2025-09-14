package com.jun.orderservice.listener;

import com.jun.orderservice.service.OrderService;
import com.jun.orderservice.entity.Order;
import com.jun.orderservice.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payment-completed", groupId = "order-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent paymentEvent) {
        try {
            logger.info("Processing payment completed event for order: {}", paymentEvent.getOrderId());

            boolean updated = orderService.updateOrderStatus(
                paymentEvent.getOrderId(),
                Order.OrderStatus.CONFIRMED,
                paymentEvent.getUserId()
            );

            if (updated) {
                logger.info("Order confirmed successfully: {}", paymentEvent.getOrderId());
            } else {
                logger.error("Failed to confirm order: {}", paymentEvent.getOrderId());
            }
        } catch (Exception e) {
            logger.error("Failed to handle payment completed event for order: {}",
                paymentEvent != null ? paymentEvent.getOrderId() : "unknown", e);
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