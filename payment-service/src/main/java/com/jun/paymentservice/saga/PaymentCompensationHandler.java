package com.jun.paymentservice.saga;

import com.jun.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentCompensationHandler {

    @Autowired
    private PaymentService paymentService;

    @KafkaListener(topics = "compensate-payment", groupId = "payment-service-compensation-group")
    public void handlePaymentCompensation(Map<String, Object> compensationData) {
        try {
            String paymentId = (String) compensationData.get("paymentId");
            String orderId = (String) compensationData.get("orderId");
            String reason = (String) compensationData.get("reason");

            System.out.println("Processing payment compensation for payment: " + paymentId + ", order: " + orderId);

            // 실제 결제 환불 처리
            Long userId = getUserIdFromPayment(paymentId); // 실제 구현에서는 결제 정보에서 userId 조회
            boolean refunded = paymentService.refundPayment(paymentId, reason, userId);

            if (refunded) {
                System.out.println("Payment compensation completed successfully for payment: " + paymentId);
            } else {
                System.err.println("Payment compensation failed for payment: " + paymentId);
            }

        } catch (Exception e) {
            System.err.println("Failed to handle payment compensation: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "payment-service-compensation-group")
    public void handleOrderCancelledCompensation(Map<String, Object> orderData) {
        try {
            String orderId = (String) orderData.get("orderId");
            Long userId = ((Number) orderData.get("userId")).longValue();
            String reason = (String) orderData.get("reason");

            System.out.println("Order cancelled: " + orderId + ". Processing payment refund...");

            // 주문 취소 시 관련된 모든 결제 환불 처리
            var payments = paymentService.getPaymentsByOrderId(orderId, userId);

            for (var payment : payments) {
                if ("COMPLETED".equals(payment.getStatus())) {
                    try {
                        paymentService.refundPayment(payment.getPaymentId(), "Order cancelled: " + reason, userId);
                        System.out.println("Refund processed for payment: " + payment.getPaymentId());
                    } catch (Exception e) {
                        System.err.println("Failed to refund payment " + payment.getPaymentId() + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to handle order cancelled compensation: " + e.getMessage());
        }
    }

    private Long getUserIdFromPayment(String paymentId) {
        // 실제 구현에서는 결제 정보에서 userId를 조회
        return 1L; // 임시값
    }
}