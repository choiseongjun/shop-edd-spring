package com.jun.orderservice.service.validator;

import com.jun.orderservice.dto.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class OrderValidator {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidator.class);

    public void validateOrderRequest(OrderRequest orderRequest, Long userId) {
        logger.debug("Validating order request for user: {}", userId);

        validateUserId(userId);
        validateOrderItems(orderRequest);
        validateShippingAddress(orderRequest);
        validatePaymentMethod(orderRequest);

        logger.debug("Order request validation passed for user: {}", userId);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }

    private void validateOrderItems(OrderRequest orderRequest) {
        if (CollectionUtils.isEmpty(orderRequest.getOrderItems())) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        orderRequest.getOrderItems().forEach(this::validateOrderItem);
    }

    private void validateOrderItem(OrderRequest.OrderItemRequest item) {
        if (item.getProductId() == null || item.getProductId() <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive number");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }

        if (item.getQuantity() > 100) { // Business rule: max 100 items per product
            throw new IllegalArgumentException("Quantity cannot exceed 100 per product");
        }
    }

    private void validateShippingAddress(OrderRequest orderRequest) {
        if (!StringUtils.hasText(orderRequest.getShippingAddress())) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        if (orderRequest.getShippingAddress().length() > 500) {
            throw new IllegalArgumentException("Shipping address cannot exceed 500 characters");
        }
    }

    private void validatePaymentMethod(OrderRequest orderRequest) {
        if (!StringUtils.hasText(orderRequest.getPaymentMethod())) {
            throw new IllegalArgumentException("Payment method is required");
        }

        // Validate allowed payment methods
        final String paymentMethod = orderRequest.getPaymentMethod().toUpperCase();
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + orderRequest.getPaymentMethod());
        }
    }

    private boolean isValidPaymentMethod(String paymentMethod) {
        return "CREDIT_CARD".equals(paymentMethod) ||
               "DEBIT_CARD".equals(paymentMethod) ||
               "BANK_TRANSFER".equals(paymentMethod) ||
               "DIGITAL_WALLET".equals(paymentMethod);
    }
}