package com.jun.productservice.saga;

import com.jun.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class StockCompensationHandler {

    @Autowired
    private ProductService productService;

    @KafkaListener(topics = "compensate-stock", groupId = "product-service-compensation-group")
    public void handleStockCompensation(Map<String, Object> compensationData) {
        try {
            String orderId = (String) compensationData.get("orderId");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> orderItems = (java.util.List<Map<String, Object>>) compensationData.get("orderItems");

            System.out.println("Processing stock compensation for order: " + orderId);

            for (Map<String, Object> item : orderItems) {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();

                try {
                    productService.releaseReservedStock(productId, quantity, orderId);
                    System.out.println("Stock compensation completed for product: " + productId + ", quantity: " + quantity);
                } catch (Exception e) {
                    System.err.println("Failed to compensate stock for product " + productId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle stock compensation: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "product-service-compensation-group")
    public void handlePaymentFailedCompensation(Map<String, Object> paymentData) {
        try {
            String orderId = (String) paymentData.get("orderId");
            System.out.println("Payment failed for order: " + orderId + ". Releasing reserved stock...");

            // 결제 실패 시 예약된 재고 해제
            // 실제 구현에서는 주문 정보를 조회해서 상품 목록을 가져와야 함
            releaseAllReservedStockForOrder(orderId);

        } catch (Exception e) {
            System.err.println("Failed to handle payment failed compensation: " + e.getMessage());
        }
    }

    private void releaseAllReservedStockForOrder(String orderId) {
        try {
            // 실제 구현에서는 주문 ID로 예약된 모든 재고를 해제해야 함
            System.out.println("Releasing all reserved stock for failed order: " + orderId);

            // 예시: Redis에서 예약된 재고 정보를 조회하고 해제
            // Map<Long, Integer> reservedItems = getReservedItemsFromCache(orderId);
            // for (Map.Entry<Long, Integer> entry : reservedItems.entrySet()) {
            //     productService.releaseReservedStock(entry.getKey(), entry.getValue(), orderId);
            // }

        } catch (Exception e) {
            System.err.println("Failed to release reserved stock for order " + orderId + ": " + e.getMessage());
        }
    }
}