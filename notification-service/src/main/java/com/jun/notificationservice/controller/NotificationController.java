package com.jun.notificationservice.controller;

import com.jun.notificationservice.dto.NotificationDto;
import com.jun.notificationservice.dto.NotificationRequest;
import com.jun.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request,
                                            @RequestHeader("User-Id") Long userId) {
        try {
            NotificationDto notification = notificationService.sendNotification(request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notification sent successfully");
            response.put("notification", notification);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@RequestHeader("User-Id") Long userId) {
        List<NotificationDto> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable String notificationId,
                                                         @RequestHeader("User-Id") Long userId) {
        NotificationDto notification = notificationService.getNotification(notificationId, userId);
        if (notification != null) {
            return ResponseEntity.ok(notification);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId,
                                      @RequestHeader("User-Id") Long userId) {
        boolean marked = notificationService.markAsRead(notificationId, userId);
        Map<String, Object> response = new HashMap<>();
        if (marked) {
            response.put("success", true);
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Notification not found");
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("User-Id") Long userId) {
        int count = notificationService.markAllAsRead(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", count + " notifications marked as read");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("User-Id") Long userId) {
        long count = notificationService.getUnreadCount(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order-placed")
    public ResponseEntity<?> handleOrderPlaced(@RequestBody Map<String, Object> orderData) {
        try {
            notificationService.handleOrderPlaced(orderData);
            Map<String, String> response = new HashMap<>();
            response.put("status", "processed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process order notification");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/payment-completed")
    public ResponseEntity<?> handlePaymentCompleted(@RequestBody Map<String, Object> paymentData) {
        try {
            notificationService.handlePaymentCompleted(paymentData);
            Map<String, String> response = new HashMap<>();
            response.put("status", "processed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process payment notification");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "notification-service");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }
}