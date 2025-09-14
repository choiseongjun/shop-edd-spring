package com.example.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private RestTemplate restTemplate;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Check for Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Validate token with user service
                boolean isValid = validateToken(token);
                if (!isValid) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user info from token
                Map<String, Object> userInfo = getUserInfoFromToken(token);
                if (userInfo != null && userInfo.containsKey("userId")) {
                    // Add user ID to request headers for downstream services
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("User-Id", userInfo.get("userId").toString())
                            .header("User-Email", (String) userInfo.get("email"))
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } else {
                    return onError(exchange, "Unable to extract user info from token", HttpStatus.UNAUTHORIZED);
                }

            } catch (Exception e) {
                return onError(exchange, "Token validation failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/api/auth/") ||
               path.contains("/api/products") && !path.contains("/reserve-stock") ||
               path.contains("/health") ||
               path.contains("/actuator") ||
               path.equals("/");
    }

    private boolean validateToken(String token) {
        try {
            String userServiceUrl = "http://user-service:8081/api/auth/validate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(headers);

            Map<String, Object> response = restTemplate.postForObject(
                userServiceUrl, entity, Map.class);

            return response != null && Boolean.TRUE.equals(response.get("valid"));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getUserInfoFromToken(String token) {
        try {
            String userServiceUrl = "http://user-service:8081/api/auth/validate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(headers);

            Map<String, Object> response = restTemplate.postForObject(
                userServiceUrl, entity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("valid"))) {
                return (Map<String, Object>) response.get("user");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"" + err + "\",\"status\":" + httpStatus.value() + "}";
        org.springframework.core.io.buffer.DataBuffer buffer =
            response.bufferFactory().wrap(body.getBytes());

        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}