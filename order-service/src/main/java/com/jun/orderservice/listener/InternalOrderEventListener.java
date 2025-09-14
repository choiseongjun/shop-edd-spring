package com.jun.orderservice.listener;

import com.jun.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class InternalOrderEventListener {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "order-cancel-request", groupId = "order-internal-group")
    public void handleOrderCancelRequest(Map<String, Object> cancelRequest) {
        try {
            String orderId = (String) cancelRequest.get("orderId");
            Long userId = ((Number) cancelRequest.get("userId")).longValue();
            String reason = (String) cancelRequest.get("reason");

            System.out.println("Processing order cancel request for: " + orderId + ", reason: " + reason);

            boolean cancelled = orderService.updateOrderStatus(orderId, "CANCELLED", userId);
            if (cancelled) {
                System.out.println("Order successfully cancelled: " + orderId);
            } else {
                System.err.println("Failed to cancel order: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle order cancel request: " + e.getMessage());
        }
    }
}