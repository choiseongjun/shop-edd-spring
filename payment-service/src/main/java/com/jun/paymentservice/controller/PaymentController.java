package com.jun.paymentservice.controller;

import com.jun.paymentservice.dto.PaymentDto;
import com.jun.paymentservice.dto.PaymentRequest;
import com.jun.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest paymentRequest,
                                          @RequestHeader("User-Id") Long userId) {
        try {
            PaymentDto payment = paymentService.processPayment(paymentRequest, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment processed successfully");
            response.put("payment", payment);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable String paymentId,
                                               @RequestHeader("User-Id") Long userId) {
        PaymentDto payment = paymentService.getPayment(paymentId, userId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByOrder(@PathVariable String orderId,
                                                             @RequestHeader("User-Id") Long userId) {
        List<PaymentDto> payments = paymentService.getPaymentsByOrderId(orderId, userId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user")
    public ResponseEntity<List<PaymentDto>> getUserPayments(@RequestHeader("User-Id") Long userId) {
        List<PaymentDto> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId,
                                         @RequestParam(required = false) String reason,
                                         @RequestHeader("User-Id") Long userId) {
        try {
            boolean refunded = paymentService.refundPayment(paymentId, reason, userId);
            Map<String, Object> response = new HashMap<>();
            if (refunded) {
                response.put("success", true);
                response.put("message", "Payment refunded successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Payment not found or cannot be refunded");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId,
                                            @RequestHeader("User-Id") Long userId) {
        try {
            String status = paymentService.getPaymentStatus(paymentId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("status", status);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/webhook/payment-completed")
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            paymentService.handlePaymentWebhook(webhookData);
            Map<String, String> response = new HashMap<>();
            response.put("status", "processed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process webhook");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "payment-service");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }
}