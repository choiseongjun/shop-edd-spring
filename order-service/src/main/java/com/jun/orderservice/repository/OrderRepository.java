package com.jun.orderservice.repository;

import com.jun.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByOrderId(String orderId);

    Order findByOrderIdAndUserId(String orderId, Long userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndFlashSaleOrderTrueOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatus(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.flashSaleOrder = true AND o.status = 'PENDING' AND o.expiresAt < :currentTime")
    List<Order> findExpiredFlashSaleOrders(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = 'PENDING'")
    long countPendingOrdersByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
}