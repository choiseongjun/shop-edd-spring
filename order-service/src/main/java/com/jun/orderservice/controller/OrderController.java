package com.jun.orderservice.controller;

import com.jun.orderservice.dto.OrderDto;
import com.jun.orderservice.dto.OrderRequest;
import com.jun.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest,
                                       @RequestHeader("User-Id") Long userId) {
        try {
            OrderDto order = orderService.createOrder(orderRequest, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Order created successfully");
            response.put("order", order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getUserOrders(@RequestHeader("User-Id") Long userId) {
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId,
                                     @RequestHeader(value = "User-Id", required = false) String userIdHeader) {
        try {
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "User-Id header is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Long userId;
            try {
                userId = Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid User-Id format");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            OrderDto order = orderService.getOrderByOrderId(orderId, userId);
            if (order != null) {
                return ResponseEntity.ok(order);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable String orderId,
                                             @RequestParam String status,
                                             @RequestHeader("User-Id") Long userId) {
        try {
            boolean updated = orderService.updateOrderStatus(orderId, status, userId);
            Map<String, Object> response = new HashMap<>();
            if (updated) {
                response.put("success", true);
                response.put("message", "Order status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Order not found or unauthorized");
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId,
                                       @RequestHeader("User-Id") Long userId) {
        try {
            boolean cancelled = orderService.cancelOrder(orderId, userId);
            Map<String, Object> response = new HashMap<>();
            if (cancelled) {
                response.put("success", true);
                response.put("message", "Order cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Order not found or cannot be cancelled");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable String orderId,
                                        @RequestHeader("User-Id") Long userId) {
        try {
            boolean confirmed = orderService.confirmOrder(orderId, userId);
            Map<String, Object> response = new HashMap<>();
            if (confirmed) {
                response.put("success", true);
                response.put("message", "Order confirmed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Order not found or cannot be confirmed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/flash-sale")
    public ResponseEntity<List<OrderDto>> getFlashSaleOrders(@RequestHeader("User-Id") Long userId) {
        List<OrderDto> orders = orderService.getFlashSaleOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "order-service");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }
}