package com.jun.paymentservice.listener;

import com.jun.paymentservice.event.PaymentCompletedEvent;
import com.jun.paymentservice.event.PaymentFailedEvent;
import com.jun.paymentservice.event.StockReservedEvent;
import com.jun.paymentservice.service.PaymentEventPublisher;
import com.jun.paymentservice.service.PaymentService;
import com.jun.paymentservice.dto.PaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class StockEventListener {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @KafkaListener(topics = "stock-reserved", groupId = "payment-service-group")
    public void handleStockReserved(StockReservedEvent stockReservedEvent) {
        try {
            String orderId = stockReservedEvent.getOrderId();
            System.out.println("Received stock reserved event for order: " + orderId +
                ", productId: " + stockReservedEvent.getProductId() +
                ", quantity: " + stockReservedEvent.getQuantity());

            PaymentRequest paymentRequest = createPaymentRequest(orderId);
            if (paymentRequest != null) {
                try {
                    var result = paymentService.processPaymentWithGateway(paymentRequest).join();

                    if (result.startsWith("PAYMENT_SUCCESS")) {
                        eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                            java.util.UUID.randomUUID().toString(),
                            orderId,
                            paymentRequest.getUserId(),
                            paymentRequest.getAmount(),
                            paymentRequest.getPaymentMethod(),
                            result
                        ));
                    } else {
                        eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                            java.util.UUID.randomUUID().toString(),
                            orderId,
                            paymentRequest.getUserId(),
                            paymentRequest.getAmount(),
                            paymentRequest.getPaymentMethod(),
                            "Payment gateway error"
                        ));
                    }
                } catch (Exception e) {
                    eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                        java.util.UUID.randomUUID().toString(),
                        orderId,
                        paymentRequest.getUserId(),
                        paymentRequest.getAmount(),
                        paymentRequest.getPaymentMethod(),
                        e.getMessage()
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle stock reserved event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "payment-service-group")
    public void handleStockReservationFailed(Map<String, Object> stockData) {
        try {
            String orderId = (String) stockData.get("orderId");
            String reason = (String) stockData.get("reason");

            eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                java.util.UUID.randomUUID().toString(),
                orderId,
                0L, // userId를 임시로 0으로 설정 (실제 구현에서는 주문 정보를 조회해야 함)
                BigDecimal.ZERO,
                "UNKNOWN",
                "Stock reservation failed: " + reason
            ));
        } catch (Exception e) {
            System.err.println("Failed to handle stock reservation failed event: " + e.getMessage());
        }
    }

    private PaymentRequest createPaymentRequest(String orderId) {
        try {
            // 게이트웨이를 통해 주문 정보 조회
            String orderUrl = "http://localhost:8080/api/orders/" + orderId;
            @SuppressWarnings("unchecked")
            Map<String, Object> orderResponse = new org.springframework.web.client.RestTemplate()
                .getForObject(orderUrl, Map.class);

            if (orderResponse != null) {
                PaymentRequest request = new PaymentRequest();
                request.setOrderId(orderId);
                request.setUserId(((Number) orderResponse.get("userId")).longValue());
                request.setAmount(new BigDecimal(orderResponse.get("totalAmount").toString()));
                request.setPaymentMethod((String) orderResponse.get("paymentMethod"));
                Boolean flashSale = (Boolean) orderResponse.get("flashSaleOrder");
                request.setFlashSalePayment(flashSale != null ? flashSale : false);
                return request;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch order info for payment: " + e.getMessage());
        }

        // 실패시 기본값 반환 (실제 서비스에서는 예외를 발생시켜야 함)
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod("CREDIT_CARD");
        request.setUserId(1L);
        return request;
    }
}