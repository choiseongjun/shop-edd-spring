package com.jun.productservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @Column(nullable = false)
    private Integer stock;
    
    @Column(name = "reserved_stock")
    private Integer reservedStock = 0;
    
    @Column(name = "category_id")
    private Long categoryId;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "flash_sale")
    private Boolean flashSale = false;
    
    @Column(name = "flash_sale_start_time")
    private LocalDateTime flashSaleStartTime;
    
    @Column(name = "flash_sale_end_time")
    private LocalDateTime flashSaleEndTime;
    
    @Column(name = "flash_sale_stock")
    private Integer flashSaleStock;
    
    @Column(name = "discount_rate")
    private Integer discountRate = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public Product() {}
    
    public Product(String name, String description, BigDecimal price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = price;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getFlashSale() {
        return flashSale;
    }

    public void setFlashSale(Boolean flashSale) {
        this.flashSale = flashSale;
    }

    public LocalDateTime getFlashSaleStartTime() {
        return flashSaleStartTime;
    }

    public void setFlashSaleStartTime(LocalDateTime flashSaleStartTime) {
        this.flashSaleStartTime = flashSaleStartTime;
    }

    public LocalDateTime getFlashSaleEndTime() {
        return flashSaleEndTime;
    }

    public void setFlashSaleEndTime(LocalDateTime flashSaleEndTime) {
        this.flashSaleEndTime = flashSaleEndTime;
    }

    public Integer getFlashSaleStock() {
        return flashSaleStock;
    }

    public void setFlashSaleStock(Integer flashSaleStock) {
        this.flashSaleStock = flashSaleStock;
    }

    public Integer getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Integer discountRate) {
        this.discountRate = discountRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getAvailableStock() {
        return stock - reservedStock;
    }
    
    public boolean isFlashSaleActive() {
        if (!flashSale) return false;
        if (flashSaleStartTime == null || flashSaleEndTime == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(flashSaleStartTime) && now.isBefore(flashSaleEndTime);
    }
}