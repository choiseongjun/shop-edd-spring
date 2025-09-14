package com.jun.orderservice.service.external;

import com.jun.orderservice.config.ServiceUrlConfig;
import com.jun.orderservice.service.OrderService.ProductInfo;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
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
    private final Tracer tracer;

    public ProductServiceClient(RestTemplate restTemplate, ServiceUrlConfig serviceUrlConfig, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.serviceUrlConfig = serviceUrlConfig;
        this.tracer = tracer;
    }

    public ProductInfo getProduct(Long productId) {
        Span productFetchSpan = tracer.nextSpan()
                .name("product-service.get-product")
                .tag("product.id", String.valueOf(productId))
                .tag("service.name", "product-service")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(productFetchSpan)) {
            final String url = serviceUrlConfig.buildFullUrl(
                    serviceUrlConfig.getProductService().getProductUrl(productId)
            );

            productFetchSpan.tag("http.url", url);
            logger.debug("Fetching product info from: {}", url);

            final Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                productFetchSpan.tag("error", "Product not found");
                throw new RuntimeException("Product not found: " + productId);
            }

            final String name = (String) response.get("name");
            final BigDecimal price = new BigDecimal(response.get("price").toString());

            productFetchSpan.tag("product.name", name);
            productFetchSpan.tag("product.price", price.toString());
            productFetchSpan.tag("http.status", "200");

            return new ProductInfo(name, price);

        } catch (Exception e) {
            productFetchSpan.tag("error", e.getMessage());
            productFetchSpan.tag("http.status", "500");
            logger.error("Failed to fetch product info for productId: {}", productId, e);
            throw new RuntimeException("Failed to validate product: " + e.getMessage());
        } finally {
            productFetchSpan.end();
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