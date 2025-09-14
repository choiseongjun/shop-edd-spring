package com.jun.paymentservice.repository;

import com.jun.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByPaymentId(String paymentId);

    Payment findByPaymentIdAndUserId(String paymentId, Long userId);

    List<Payment> findByOrderIdAndUserId(String orderId, Long userId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.flashSalePayment = true AND p.status = 'COMPLETED'")
    List<Payment> findCompletedFlashSalePayments();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaymentsByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'FAILED' AND p.createdAt >= :since")
    long countFailedPaymentsSince(@Param("since") LocalDateTime since);
}