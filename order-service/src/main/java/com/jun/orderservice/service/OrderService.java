package com.jun.orderservice.service;

import com.jun.orderservice.dto.OrderDto;
import com.jun.orderservice.dto.OrderRequest;
import com.jun.orderservice.entity.Order;
import com.jun.orderservice.entity.OrderItem;
import com.jun.orderservice.event.OrderCreatedEvent;
import com.jun.orderservice.event.OrderCancelledEvent;
import com.jun.orderservice.repository.OrderRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Transactional
    public OrderDto createOrder(OrderRequest orderRequest, Long userId) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setFlashSaleOrder(orderRequest.getFlashSaleOrder());
        order.setStatus(Order.OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderCreatedEvent.OrderItemInfo> orderItemInfos = new java.util.ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            try {
                String productUrl = "http://localhost:8080/api/products/" + itemRequest.getProductId();
                Map<String, Object> productResponse = restTemplate.getForObject(productUrl, Map.class);

                if (productResponse != null) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setFlashSaleItem(itemRequest.getFlashSaleItem());
                    orderItem.setProductName((String) productResponse.get("name"));
                    orderItem.setUnitPrice(new BigDecimal(productResponse.get("price").toString()));
                    orderItem.setTotalPrice(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

                    totalAmount = totalAmount.add(orderItem.getTotalPrice());
                    order.getOrderItems().add(orderItem);

                    // 이벤트를 위한 주문 아이템 정보 생성
                    orderItemInfos.add(new OrderCreatedEvent.OrderItemInfo(
                        itemRequest.getProductId(),
                        itemRequest.getQuantity(),
                        orderItem.getProductName(),
                        orderItem.getUnitPrice(),
                        itemRequest.getFlashSaleItem()
                    ));
                } else {
                    throw new RuntimeException("Product not found: " + itemRequest.getProductId());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to validate product: " + e.getMessage());
            }
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 주문 생성 이벤트 발행
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
            savedOrder.getOrderId(),
            userId,
            savedOrder.getShippingAddress(),
            savedOrder.getPaymentMethod(),
            savedOrder.getTotalAmount(),
            orderItemInfos,
            savedOrder.getFlashSaleOrder()
        );

        eventPublisher.publishOrderCreated(orderCreatedEvent);

        return new OrderDto(savedOrder);
    }

    public List<OrderDto> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(OrderDto::new).collect(Collectors.toList());
    }

    public OrderDto getOrderByOrderId(String orderId, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId);
        return order != null ? new OrderDto(order) : null;
    }

    @Transactional
    public boolean updateOrderStatus(String orderId, String status, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId);
        if (order != null) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                order.setStatus(orderStatus);
                orderRepository.save(order);
                return true;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid order status: " + status);
            }
        }
        return false;
    }

    @Transactional
    public boolean updateOrderStatus(String orderId, Order.OrderStatus status, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId);
        if (order != null) {
            order.setStatus(status);
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean cancelOrder(String orderId, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId);
        if (order != null && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            
            // Release reserved stock for each order item
            for (OrderItem item : order.getOrderItems()) {
                try {
                    String releaseUrl = "http://localhost:8080/api/products/release-reserved-stock" +
                            "?orderId=" + orderId + 
                            "&productId=" + item.getProductId() + 
                            "&quantity=" + item.getQuantity();
                    restTemplate.postForObject(releaseUrl, null, Map.class);
                } catch (Exception e) {
                    // Log error but don't fail the cancellation
                    System.err.println("Failed to release stock for product " + item.getProductId() + ": " + e.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean confirmOrder(String orderId, Long userId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId);
        if (order != null && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    public List<OrderDto> getFlashSaleOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdAndFlashSaleOrderTrueOrderByCreatedAtDesc(userId);
        return orders.stream().map(OrderDto::new).collect(Collectors.toList());
    }

    @Transactional
    public Order createFlashSaleOrder(Long userId, Long productId, Integer quantity) {
        String lockKey = "flash_sale_lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("Unable to acquire lock for product: " + productId);
            }

            Order order = new Order();
            order.setOrderId(UUID.randomUUID().toString());
            order.setUserId(userId);
            order.setFlashSaleOrder(true);
            order.setStatus(Order.OrderStatus.PENDING);

            return orderRepository.save(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}