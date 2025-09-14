package com.jun.paymentservice.service;

import com.jun.paymentservice.dto.PaymentDto;
import com.jun.paymentservice.dto.PaymentRequest;
import com.jun.paymentservice.entity.Payment;
import com.jun.paymentservice.event.PaymentCompletedEvent;
import com.jun.paymentservice.event.PaymentFailedEvent;
import com.jun.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentDto processPayment(PaymentRequest paymentRequest, Long userId) {
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentRequest.getPaymentMethod().toUpperCase()));
        payment.setFlashSalePayment(paymentRequest.getFlashSalePayment());
        payment.setStatus(Payment.PaymentStatus.PROCESSING);

        Payment savedPayment = paymentRepository.save(payment);

        try {
            String result = processPaymentWithGateway(paymentRequest).join();

            if (result.startsWith("PAYMENT_SUCCESS")) {
                savedPayment.setStatus(Payment.PaymentStatus.COMPLETED);
                savedPayment.setTransactionId(result);

                // 이벤트 발행으로 대체
                eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    savedPayment.getPaymentId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount(),
                    savedPayment.getPaymentMethod().name(),
                    savedPayment.getTransactionId()
                ));

            } else {
                savedPayment.setStatus(Payment.PaymentStatus.FAILED);
                savedPayment.setFailureReason("Payment gateway rejected the transaction");

                eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                    savedPayment.getPaymentId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount(),
                    savedPayment.getPaymentMethod().name(),
                    "Payment gateway rejected the transaction"
                ));
            }
        } catch (Exception e) {
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason(e.getMessage());

            eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                savedPayment.getPaymentId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod().name(),
                e.getMessage()
            ));
        }

        return new PaymentDto(paymentRepository.save(savedPayment));
    }

    public PaymentDto getPayment(String paymentId, Long userId) {
        Payment payment = paymentRepository.findByPaymentIdAndUserId(paymentId, userId);
        return payment != null ? new PaymentDto(payment) : null;
    }

    public List<PaymentDto> getPaymentsByOrderId(String orderId, Long userId) {
        List<Payment> payments = paymentRepository.findByOrderIdAndUserId(orderId, userId);
        return payments.stream().map(PaymentDto::new).collect(Collectors.toList());
    }

    public List<PaymentDto> getPaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return payments.stream().map(PaymentDto::new).collect(Collectors.toList());
    }

    @Transactional
    public boolean refundPayment(String paymentId, String reason, Long userId) {
        Payment payment = paymentRepository.findByPaymentIdAndUserId(paymentId, userId);
        if (payment != null && payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setRefundReason(reason);
            payment.setRefundedAmount(payment.getAmount());
            paymentRepository.save(payment);

            // 이벤트 발행으로 대체 (필요한 경우 환불 이벤트 추가 가능)

            return true;
        }
        return false;
    }

    public String getPaymentStatus(String paymentId, Long userId) {
        Payment payment = paymentRepository.findByPaymentIdAndUserId(paymentId, userId);
        if (payment != null) {
            return payment.getStatus().name();
        }
        throw new RuntimeException("Payment not found");
    }

    public void handlePaymentWebhook(Map<String, Object> webhookData) {
        String paymentId = (String) webhookData.get("paymentId");
        String status = (String) webhookData.get("status");
        String transactionId = (String) webhookData.get("transactionId");

        Payment payment = paymentRepository.findByPaymentId(paymentId);
        if (payment != null) {
            payment.setStatus(Payment.PaymentStatus.valueOf(status.toUpperCase()));
            payment.setTransactionId(transactionId);
            paymentRepository.save(payment);

            if ("COMPLETED".equals(status)) {
                eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    payment.getPaymentId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getPaymentMethod().name(),
                    transactionId
                ));
            }
        }
    }

    @CircuitBreaker(name = "external-payment-api", fallbackMethod = "fallbackPayment")
    @Retry(name = "external-payment-api")
    @TimeLimiter(name = "external-payment-api")
    public CompletableFuture<String> processPaymentWithGateway(PaymentRequest paymentRequest) {
        return CompletableFuture.supplyAsync(() -> {
            // 외부 결제 API 호출 시뮬레이션
            if (Math.random() > 0.8) { // 20% 실패율로 서킷브레이커 테스트
                throw new RuntimeException("Payment gateway error");
            }

            // 결제 성공 시뮬레이션
            return "PAYMENT_SUCCESS_" + UUID.randomUUID().toString();
        });
    }

    public CompletableFuture<String> fallbackPayment(PaymentRequest paymentRequest, Exception ex) {
        return CompletableFuture.completedFuture("PAYMENT_PENDING_" + paymentRequest.getOrderId());
    }

    private void updateOrderStatus(String orderId, String status) {
        try {
            String orderServiceUrl = "http://order-service:8083/api/orders/" + orderId + "/status?status=" + status;
            restTemplate.put(orderServiceUrl, null);
        } catch (Exception e) {
            // Log error but don't fail the payment
            System.err.println("Failed to update order status: " + e.getMessage());
        }
    }
}