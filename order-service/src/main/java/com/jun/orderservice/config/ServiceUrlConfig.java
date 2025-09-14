package com.jun.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceUrlConfig {

    private String gatewayUrl = "http://localhost:8080";
    private ProductService productService = new ProductService();
    private PaymentService paymentService = new PaymentService();

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public ProductService getProductService() {
        return productService;
    }

    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public static class ProductService {
        private String baseUrl = "/api/products";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getProductUrl(Long productId) {
            return baseUrl + "/" + productId;
        }

        public String getStockReservationUrl() {
            return baseUrl + "/reserve-stock";
        }
    }

    public static class PaymentService {
        private String baseUrl = "/api/payments";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getProcessPaymentUrl() {
            return baseUrl + "/process";
        }
    }

    public String buildFullUrl(String relativePath) {
        return gatewayUrl + relativePath;
    }
}