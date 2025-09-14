package com.jun.productservice.repository;

import com.jun.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByActiveTrue();
    
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);
    
    @Query("SELECT p FROM Product p WHERE p.flashSale = true AND p.active = true AND :now BETWEEN p.flashSaleStartTime AND p.flashSaleEndTime")
    List<Product> findActiveFlashSaleProducts(@Param("now") LocalDateTime now);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock + :quantity WHERE p.id = :id")
    int reserveStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    @Modifying
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock - :quantity, p.stock = p.stock - :quantity WHERE p.id = :id")
    int confirmStockReduction(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    @Modifying
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock - :quantity WHERE p.id = :id")
    int releaseReservedStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.active = true ORDER BY p.createdAt DESC")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword% AND p.active = true")
    List<Product> findByKeyword(@Param("keyword") String keyword);
}