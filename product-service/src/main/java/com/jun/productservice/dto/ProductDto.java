package com.jun.productservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer availableStock;
    private Long categoryId;
    private String imageUrl;
    private Boolean active;
    private Boolean flashSale;
    private LocalDateTime flashSaleStartTime;
    private LocalDateTime flashSaleEndTime;
    private Integer flashSaleStock;
    private Integer discountRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean flashSaleActive;

    public ProductDto() {}

    public ProductDto(Long id, String name, String description, BigDecimal price, 
                     BigDecimal originalPrice, Integer stock, Integer availableStock,
                     Long categoryId, String imageUrl, Boolean active, Boolean flashSale,
                     LocalDateTime flashSaleStartTime, LocalDateTime flashSaleEndTime,
                     Integer flashSaleStock, Integer discountRate, LocalDateTime createdAt,
                     LocalDateTime updatedAt, Boolean flashSaleActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.availableStock = availableStock;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.active = active;
        this.flashSale = flashSale;
        this.flashSaleStartTime = flashSaleStartTime;
        this.flashSaleEndTime = flashSaleEndTime;
        this.flashSaleStock = flashSaleStock;
        this.discountRate = discountRate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.flashSaleActive = flashSaleActive;
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

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
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

    public Boolean getFlashSaleActive() {
        return flashSaleActive;
    }

    public void setFlashSaleActive(Boolean flashSaleActive) {
        this.flashSaleActive = flashSaleActive;
    }
}