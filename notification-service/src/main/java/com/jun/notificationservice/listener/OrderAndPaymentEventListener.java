package com.jun.notificationservice.listener;

import com.jun.notificationservice.event.NotificationEvent;
import com.jun.notificationservice.service.NotificationEventPublisher;
import com.jun.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderAndPaymentEventListener {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationEventPublisher eventPublisher;

    @KafkaListener(topics = "order-created", groupId = "notification-service-group")
    public void handleOrderCreated(Map<String, Object> orderData) {
        try {
            String orderId = (String) orderData.get("orderId");
            Long userId = ((Number) orderData.get("userId")).longValue();

            NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID().toString(),
                userId,
                "ORDER_CREATED",
                "주문이 생성되었습니다",
                "주문번호 " + orderId + "가 성공적으로 생성되었습니다. 결제 처리 중입니다.",
                orderId
            );

            notificationService.createNotification(notification);
            eventPublisher.publishNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to handle order created notification: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(Map<String, Object> paymentData) {
        try {
            String orderId = (String) paymentData.get("orderId");
            Long userId = ((Number) paymentData.get("userId")).longValue();
            String amount = paymentData.get("amount").toString();

            NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID().toString(),
                userId,
                "PAYMENT_COMPLETED",
                "결제가 완료되었습니다",
                "주문번호 " + orderId + "의 결제(" + amount + "원)가 성공적으로 완료되었습니다.",
                orderId
            );

            notificationService.createNotification(notification);
            eventPublisher.publishNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to handle payment completed notification: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "notification-service-group")
    public void handlePaymentFailed(Map<String, Object> paymentData) {
        try {
            String orderId = (String) paymentData.get("orderId");
            Long userId = ((Number) paymentData.get("userId")).longValue();
            String failureReason = (String) paymentData.get("failureReason");

            NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID().toString(),
                userId,
                "PAYMENT_FAILED",
                "결제에 실패했습니다",
                "주문번호 " + orderId + "의 결제가 실패했습니다. 사유: " + failureReason,
                orderId
            );

            notificationService.createNotification(notification);
            eventPublisher.publishNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to handle payment failed notification: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "notification-service-group")
    public void handleStockReservationFailed(Map<String, Object> stockData) {
        try {
            String orderId = (String) stockData.get("orderId");
            String reason = (String) stockData.get("reason");
            Long productId = ((Number) stockData.get("productId")).longValue();

            // userId를 얻기 위해 주문 정보를 조회해야 하지만, 여기서는 임시로 처리
            NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID().toString(),
                1L, // 임시 userId
                "STOCK_SHORTAGE",
                "재고가 부족합니다",
                "주문번호 " + orderId + "의 상품(ID: " + productId + ")의 재고가 부족합니다. " + reason,
                orderId
            );

            notificationService.createNotification(notification);
            eventPublisher.publishNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to handle stock reservation failed notification: " + e.getMessage());
        }
    }
}