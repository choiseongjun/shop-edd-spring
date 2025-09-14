package com.jun.orderservice.service;

import com.jun.orderservice.config.ServiceUrlConfig;
import com.jun.orderservice.dto.OrderDto;
import com.jun.orderservice.dto.OrderRequest;
import com.jun.orderservice.entity.Order;
import com.jun.orderservice.entity.OrderItem;
import com.jun.orderservice.event.OrderCreatedEvent;
import com.jun.orderservice.repository.OrderRepository;
import com.jun.orderservice.service.external.ProductServiceClient;
import com.jun.orderservice.service.validator.OrderValidator;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final int LOCK_WAIT_TIME_SECONDS = 3;
    private static final int LOCK_LEASE_TIME_SECONDS = 5;

    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;
    private final ProductServiceClient productServiceClient;
    private final OrderEventPublisher eventPublisher;
    private final ServiceUrlConfig serviceUrlConfig;
    private final OrderValidator orderValidator;
    private final Tracer tracer;

    public OrderService(OrderRepository orderRepository,
                       RedissonClient redissonClient,
                       ProductServiceClient productServiceClient,
                       OrderEventPublisher eventPublisher,
                       ServiceUrlConfig serviceUrlConfig,
                       OrderValidator orderValidator,
                       Tracer tracer) {
        this.orderRepository = orderRepository;
        this.redissonClient = redissonClient;
        this.productServiceClient = productServiceClient;
        this.eventPublisher = eventPublisher;
        this.serviceUrlConfig = serviceUrlConfig;
        this.orderValidator = orderValidator;
        this.tracer = tracer;
    }

    @Transactional
    @Observed(name = "order.create", contextualName = "order-creation")
    public OrderDto createOrder(OrderRequest orderRequest, Long userId) {
        Span orderCreationSpan = tracer.nextSpan()
                .name("order.create")
                .tag("user.id", String.valueOf(userId))
                .tag("order.items.count", String.valueOf(orderRequest.getOrderItems().size()))
                .tag("order.flash.sale", String.valueOf(orderRequest.getFlashSaleOrder()))
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(orderCreationSpan)) {
            logger.info("Creating order for user: {}", userId);

            // 주문 검증
            Span validationSpan = tracer.nextSpan()
                    .name("order.validation")
                    .tag("user.id", String.valueOf(userId))
                    .start();
            try (Tracer.SpanInScope validationScope = tracer.withSpan(validationSpan)) {
                orderValidator.validateOrderRequest(orderRequest, userId);
            } finally {
                validationSpan.end();
            }

            // 주문 빌드
            final Order order = buildOrder(orderRequest, userId);

            // 주문 아이템 처리 (상품 정보 조회 포함)
            Span itemProcessingSpan = tracer.nextSpan()
                    .name("order.items.processing")
                    .tag("items.count", String.valueOf(orderRequest.getOrderItems().size()))
                    .start();
            final List<OrderCreatedEvent.OrderItemInfo> orderItemInfos;
            try (Tracer.SpanInScope itemScope = tracer.withSpan(itemProcessingSpan)) {
                orderItemInfos = processOrderItems(order, orderRequest);
            } finally {
                itemProcessingSpan.tag("order.total.amount", order.getTotalAmount().toString());
                itemProcessingSpan.end();
            }

            // 주문 저장
            Span saveSpan = tracer.nextSpan()
                    .name("order.save")
                    .tag("order.id", order.getOrderId())
                    .start();
            final Order savedOrder;
            try (Tracer.SpanInScope saveScope = tracer.withSpan(saveSpan)) {
                savedOrder = orderRepository.save(order);
            } finally {
                saveSpan.end();
            }

            // 이벤트 발행
            Span eventSpan = tracer.nextSpan()
                    .name("order.event.publish")
                    .tag("order.id", savedOrder.getOrderId())
                    .tag("event.type", "OrderCreated")
                    .start();
            try (Tracer.SpanInScope eventScope = tracer.withSpan(eventSpan)) {
                publishOrderCreatedEvent(savedOrder, userId, orderItemInfos);
            } finally {
                eventSpan.end();
            }

            orderCreationSpan.tag("order.id", savedOrder.getOrderId());
            orderCreationSpan.tag("order.status", savedOrder.getStatus().toString());
            logger.info("Successfully created order: {} for user: {}", savedOrder.getOrderId(), userId);
            return new OrderDto(savedOrder);

        } catch (Exception e) {
            orderCreationSpan.tag("error", e.getMessage());
            logger.error("Failed to create order for user: {}", userId, e);
            throw e;
        } finally {
            orderCreationSpan.end();
        }
    }

    private Order buildOrder(OrderRequest orderRequest, Long userId) {
        return Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(userId)
                .shippingAddress(orderRequest.getShippingAddress())
                .paymentMethod(orderRequest.getPaymentMethod())
                .flashSaleOrder(orderRequest.getFlashSaleOrder())
                .status(Order.OrderStatus.PENDING)
                .build();
    }

    private List<OrderCreatedEvent.OrderItemInfo> processOrderItems(Order order, OrderRequest orderRequest) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            final var productInfo = productServiceClient.getProduct(itemRequest.getProductId());
            final OrderItem orderItem = createOrderItem(order, itemRequest, productInfo);

            totalAmount = totalAmount.add(orderItem.getTotalPrice());
            order.addOrderItem(orderItem);
        }

        order.updateTotalAmount(totalAmount);

        return order.getOrderItems().stream()
                .map(this::mapToOrderItemInfo)
                .collect(Collectors.toList());
    }

    private OrderItem createOrderItem(Order order, OrderRequest.OrderItemRequest itemRequest, ProductInfo productInfo) {
        return OrderItem.builder()
                .order(order)
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .flashSaleItem(itemRequest.getFlashSaleItem())
                .productName(productInfo.getName())
                .unitPrice(productInfo.getPrice())
                .build();
    }

    private OrderCreatedEvent.OrderItemInfo mapToOrderItemInfo(OrderItem orderItem) {
        return new OrderCreatedEvent.OrderItemInfo(
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getFlashSaleItem()
        );
    }

    private void publishOrderCreatedEvent(Order order, Long userId, List<OrderCreatedEvent.OrderItemInfo> orderItemInfos) {
        final OrderCreatedEvent event = new OrderCreatedEvent(
                order.getOrderId(),
                userId,
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                orderItemInfos,
                order.getFlashSaleOrder()
        );

        eventPublisher.publishOrderCreated(event);
        logger.debug("Published order created event for order: {}", order.getOrderId());
    }

    public List<OrderDto> getOrdersByUserId(Long userId) {
        logger.debug("Retrieving orders for user: {}", userId);
        final List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderByOrderId(String orderId, Long userId) {
        logger.debug("Retrieving order: {} for user: {}", orderId, userId);
        final Order order = findOrderByIdAndUser(orderId, userId);
        return order != null ? new OrderDto(order) : null;
    }

    @Transactional
    public boolean updateOrderStatus(String orderId, String status, Long userId) {
        try {
            final Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return updateOrderStatus(orderId, orderStatus, userId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order status: {}", status);
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    @Transactional
    public boolean updateOrderStatus(String orderId, Order.OrderStatus status, Long userId) {
        logger.info("Updating order status: {} to {} for user: {}", orderId, status, userId);

        final Order order = findOrderByIdAndUser(orderId, userId);
        if (order == null) {
            logger.warn("Order not found: {} for user: {}", orderId, userId);
            return false;
        }

        order.updateStatus(status);
        orderRepository.save(order);

        logger.info("Successfully updated order status: {} to {}", orderId, status);
        return true;
    }

    @Transactional
    public boolean cancelOrder(String orderId, Long userId) {
        logger.info("Cancelling order: {} for user: {}", orderId, userId);

        final Order order = findOrderByIdAndUser(orderId, userId);
        if (order == null || !order.canBeCancelled()) {
            logger.warn("Order cannot be cancelled: {} for user: {}", orderId, userId);
            return false;
        }

        order.cancel();
        orderRepository.save(order);

        releaseReservedStock(order);

        logger.info("Successfully cancelled order: {}", orderId);
        return true;
    }

    private void releaseReservedStock(Order order) {
        order.getOrderItems().forEach(item -> {
            try {
                productServiceClient.releaseReservedStock(order.getOrderId(), item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                logger.error("Failed to release stock for product {} in order {}: {}",
                           item.getProductId(), order.getOrderId(), e.getMessage());
            }
        });
    }

    @Transactional
    public boolean confirmOrder(String orderId, Long userId) {
        logger.info("Confirming order: {} for user: {}", orderId, userId);

        final Order order = findOrderByIdAndUser(orderId, userId);
        if (order == null || !order.canBeConfirmed()) {
            logger.warn("Order cannot be confirmed: {} for user: {}", orderId, userId);
            return false;
        }

        order.confirm();
        orderRepository.save(order);

        logger.info("Successfully confirmed order: {}", orderId);
        return true;
    }

    public List<OrderDto> getFlashSaleOrdersByUserId(Long userId) {
        logger.debug("Retrieving flash sale orders for user: {}", userId);
        final List<Order> orders = orderRepository.findByUserIdAndFlashSaleOrderTrueOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Order createFlashSaleOrder(Long userId, Long productId, Integer quantity) {
        logger.info("Creating flash sale order for user: {}, product: {}", userId, productId);

        final String lockKey = buildFlashSaleLockKey(productId);
        final RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("Unable to acquire lock for product: " + productId);
            }

            final Order order = Order.builder()
                    .orderId(UUID.randomUUID().toString())
                    .userId(userId)
                    .flashSaleOrder(true)
                    .status(Order.OrderStatus.PENDING)
                    .build();

            final Order savedOrder = orderRepository.save(order);
            logger.info("Successfully created flash sale order: {}", savedOrder.getOrderId());
            return savedOrder;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            releaseLockSafely(lock);
        }
    }

    private Order findOrderByIdAndUser(String orderId, Long userId) {
        return orderRepository.findByOrderIdAndUserId(orderId, userId);
    }

    private String buildFlashSaleLockKey(Long productId) {
        return "flash_sale_lock:" + productId;
    }

    private void releaseLockSafely(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                logger.error("Error releasing lock: {}", e.getMessage());
            }
        }
    }

    // Product info DTO for internal use
    public static class ProductInfo {
        private final String name;
        private final BigDecimal price;

        public ProductInfo(String name, BigDecimal price) {
            this.name = name;
            this.price = price;
        }

        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
    }
}