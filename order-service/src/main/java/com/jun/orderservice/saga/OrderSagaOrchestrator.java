package com.jun.orderservice.saga;

import com.jun.orderservice.event.OrderCreatedEvent;
import com.jun.orderservice.event.OrderCancelledEvent;
import com.jun.orderservice.event.StockReservedEvent;
import com.jun.orderservice.event.PaymentCompletedEvent;
import com.jun.orderservice.service.OrderEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderSagaOrchestrator {

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();

    @KafkaListener(topics = "order-created", groupId = "order-saga-orchestrator-group-v2")
    public void startOrderSaga(OrderCreatedEvent orderEvent) {
        try {
            String orderId = orderEvent.getOrderId();
            SagaState sagaState = new SagaState(orderId);
            sagaState.setStep(SagaStep.ORDER_CREATED);
            sagaStates.put(orderId, sagaState);

            System.out.println("Order Saga started for order: " + orderId);
        } catch (Exception e) {
            System.err.println("Failed to start order saga: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "stock-reserved", groupId = "order-saga-group")
    public void handleStockReserved(StockReservedEvent stockReservedEvent) {
        String orderId = stockReservedEvent.getOrderId();
        SagaState sagaState = sagaStates.get(orderId);

        if (sagaState != null) {
            sagaState.setStep(SagaStep.STOCK_RESERVED);
            sagaState.addCompensationAction(() -> {
                // 재고 해제를 위한 보상 액션
                releaseStock(orderId);
            });
            System.out.println("Stock reserved for order: " + orderId +
                ", productId: " + stockReservedEvent.getProductId() +
                ", quantity: " + stockReservedEvent.getQuantity());
        }
    }

    @KafkaListener(topics = "payment-completed", groupId = "order-saga-group")
    public void handlePaymentCompleted(PaymentCompletedEvent paymentEvent) {
        String orderId = paymentEvent.getOrderId();
        SagaState sagaState = sagaStates.get(orderId);

        if (sagaState != null) {
            sagaState.setStep(SagaStep.PAYMENT_COMPLETED);
            sagaState.addCompensationAction(() -> {
                // 결제 취소를 위한 보상 액션
                refundPayment(orderId);
            });

            // Saga 성공 완료
            sagaState.setCompleted(true);
            System.out.println("Order Saga completed successfully for order: " + orderId);
        }
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "order-saga-group")
    public void handleStockReservationFailed(Map<String, Object> stockData) {
        String orderId = (String) stockData.get("orderId");
        SagaState sagaState = sagaStates.get(orderId);

        if (sagaState != null) {
            System.out.println("Stock reservation failed for order: " + orderId + ". Starting compensation...");
            executeCompensation(sagaState);
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-saga-group")
    public void handlePaymentFailed(Map<String, Object> paymentData) {
        String orderId = (String) paymentData.get("orderId");
        SagaState sagaState = sagaStates.get(orderId);

        if (sagaState != null) {
            System.out.println("Payment failed for order: " + orderId + ". Starting compensation...");
            executeCompensation(sagaState);
        }
    }

    private void executeCompensation(SagaState sagaState) {
        System.out.println("Executing compensation for order: " + sagaState.getOrderId());

        // 역순으로 보상 액션 실행
        for (int i = sagaState.getCompensationActions().size() - 1; i >= 0; i--) {
            try {
                sagaState.getCompensationActions().get(i).run();
            } catch (Exception e) {
                System.err.println("Compensation action failed: " + e.getMessage());
            }
        }

        // 주문 취소를 위한 이벤트 발행 (직접 OrderService 호출 대신)
        try {
            String orderId = sagaState.getOrderId();

            // 주문 취소 요청을 위한 내부 이벤트 발행
            Map<String, Object> cancelRequest = Map.of(
                "orderId", orderId,
                "reason", "Saga compensation",
                "userId", 1L // 임시값 - 실제 구현에서는 saga state에서 관리
            );

            kafkaTemplate.send("order-cancel-request", orderId, cancelRequest);

            // 주문 취소 이벤트 발행
            eventPublisher.publishOrderCancelled(createOrderCancelledEvent(orderId));
        } catch (Exception e) {
            System.err.println("Failed to cancel order during compensation: " + e.getMessage());
        }

        sagaState.setCompensated(true);
        System.out.println("Compensation completed for order: " + sagaState.getOrderId());
    }

    private void releaseStock(String orderId) {
        System.out.println("Releasing stock for order: " + orderId);
        // 실제 구현에서는 상품 서비스에 재고 해제 요청
    }

    private void refundPayment(String orderId) {
        System.out.println("Refunding payment for order: " + orderId);
        // 실제 구현에서는 결제 서비스에 환불 요청
    }

    private Long getUserIdFromOrder(String orderId) {
        // 실제 구현에서는 주문 정보에서 userId를 조회
        return 1L; // 임시값
    }

    private OrderCancelledEvent createOrderCancelledEvent(String orderId) {
        // 실제 구현에서는 주문 정보를 조회해서 이벤트 생성
        return new OrderCancelledEvent(
            orderId,
            1L, // 임시 userId
            "Saga compensation",
            java.util.Collections.emptyList(),
            java.math.BigDecimal.ZERO
        );
    }

    public enum SagaStep {
        ORDER_CREATED,
        STOCK_RESERVED,
        PAYMENT_COMPLETED
    }

    public static class SagaState {
        private final String orderId;
        private SagaStep step;
        private boolean completed = false;
        private boolean compensated = false;
        private final java.util.List<Runnable> compensationActions = new java.util.ArrayList<>();

        public SagaState(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderId() { return orderId; }
        public SagaStep getStep() { return step; }
        public void setStep(SagaStep step) { this.step = step; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public boolean isCompensated() { return compensated; }
        public void setCompensated(boolean compensated) { this.compensated = compensated; }
        public java.util.List<Runnable> getCompensationActions() { return compensationActions; }
        public void addCompensationAction(Runnable action) { compensationActions.add(action); }
    }
}