package com.jun.orderservice.service.external;

import com.jun.orderservice.config.ServiceUrlConfig;
import com.jun.orderservice.service.OrderService.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;
    private final ServiceUrlConfig serviceUrlConfig;

    public ProductServiceClient(RestTemplate restTemplate, ServiceUrlConfig serviceUrlConfig) {
        this.restTemplate = restTemplate;
        this.serviceUrlConfig = serviceUrlConfig;
    }

    public ProductInfo getProduct(Long productId) {
        try {
            final String url = serviceUrlConfig.buildFullUrl(
                    serviceUrlConfig.getProductService().getProductUrl(productId)
            );

            logger.debug("Fetching product info from: {}", url);

            final Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            final String name = (String) response.get("name");
            final BigDecimal price = new BigDecimal(response.get("price").toString());

            return new ProductInfo(name, price);

        } catch (Exception e) {
            logger.error("Failed to fetch product info for productId: {}", productId, e);
            throw new RuntimeException("Failed to validate product: " + e.getMessage());
        }
    }

    public void releaseReservedStock(String orderId, Long productId, Integer quantity) {
        try {
            final String url = serviceUrlConfig.buildFullUrl("/api/products/release-reserved-stock") +
                    "?orderId=" + orderId +
                    "&productId=" + productId +
                    "&quantity=" + quantity;

            logger.debug("Releasing reserved stock: {}", url);

            restTemplate.postForObject(url, null, Map.class);

            logger.debug("Successfully released stock for product: {} in order: {}", productId, orderId);

        } catch (Exception e) {
            logger.error("Failed to release stock for product {} in order {}: {}",
                        productId, orderId, e.getMessage());
            throw e;
        }
    }
}