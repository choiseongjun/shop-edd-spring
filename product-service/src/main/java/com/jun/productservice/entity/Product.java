package com.jun.productservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    protected Product() {}

    private Product(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.price = builder.price;
        this.originalPrice = builder.originalPrice != null ? builder.originalPrice : builder.price;
        this.stock = builder.stock;
        this.reservedStock = builder.reservedStock;
        this.categoryId = builder.categoryId;
        this.imageUrl = builder.imageUrl;
        this.active = builder.active;
        this.flashSale = builder.flashSale;
        this.flashSaleStartTime = builder.flashSaleStartTime;
        this.flashSaleEndTime = builder.flashSaleEndTime;
        this.flashSaleStock = builder.flashSaleStock;
        this.discountRate = builder.discountRate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private Integer reservedStock = 0;
        private Long categoryId;
        private String imageUrl;
        private Boolean active = true;
        private Boolean flashSale = false;
        private LocalDateTime flashSaleStartTime;
        private LocalDateTime flashSaleEndTime;
        private Integer flashSaleStock;
        private Integer discountRate = 0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder originalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
            return this;
        }

        public Builder stock(Integer stock) {
            this.stock = stock;
            return this;
        }

        public Builder reservedStock(Integer reservedStock) {
            this.reservedStock = reservedStock;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder flashSale(Boolean flashSale) {
            this.flashSale = flashSale;
            return this;
        }

        public Builder flashSaleStartTime(LocalDateTime flashSaleStartTime) {
            this.flashSaleStartTime = flashSaleStartTime;
            return this;
        }

        public Builder flashSaleEndTime(LocalDateTime flashSaleEndTime) {
            this.flashSaleEndTime = flashSaleEndTime;
            return this;
        }

        public Builder flashSaleStock(Integer flashSaleStock) {
            this.flashSaleStock = flashSaleStock;
            return this;
        }

        public Builder discountRate(Integer discountRate) {
            this.discountRate = discountRate;
            return this;
        }

        public Product build() {
            validateRequiredFields();
            return new Product(this);
        }

        private void validateRequiredFields() {
            Objects.requireNonNull(name, "Product name cannot be null");
            Objects.requireNonNull(price, "Product price cannot be null");
            Objects.requireNonNull(stock, "Product stock cannot be null");

            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Product price cannot be negative");
            }
            if (stock < 0) {
                throw new IllegalArgumentException("Product stock cannot be negative");
            }
            if (reservedStock < 0) {
                throw new IllegalArgumentException("Reserved stock cannot be negative");
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void updateStock(Integer newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        if (newStock < reservedStock) {
            throw new IllegalArgumentException("Stock cannot be less than reserved stock");
        }
        this.stock = newStock;
    }

    public void reserveStock(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (getAvailableStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock available");
        }
        this.reservedStock += quantity;
    }

    public void releaseReservedStock(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedStock < quantity) {
            throw new IllegalArgumentException("Cannot release more than reserved");
        }
        this.reservedStock -= quantity;
    }

    public void confirmStockReduction(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedStock < quantity) {
            throw new IllegalArgumentException("Cannot confirm more than reserved");
        }
        this.stock -= quantity;
        this.reservedStock -= quantity;
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.price = newPrice;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean hasAvailableStock(Integer requiredQuantity) {
        return getAvailableStock() >= requiredQuantity;
    }

    // Public setters for service layer access
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setReservedStock(Integer reservedStock) { this.reservedStock = reservedStock; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setActive(Boolean active) { this.active = active; }
    public void setFlashSale(Boolean flashSale) { this.flashSale = flashSale; }
    public void setFlashSaleStartTime(LocalDateTime flashSaleStartTime) { this.flashSaleStartTime = flashSaleStartTime; }
    public void setFlashSaleEndTime(LocalDateTime flashSaleEndTime) { this.flashSaleEndTime = flashSaleEndTime; }
    public void setFlashSaleStock(Integer flashSaleStock) { this.flashSaleStock = flashSaleStock; }
    public void setDiscountRate(Integer discountRate) { this.discountRate = discountRate; }

    // Public getters (immutable after creation, except for business methods)
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public Integer getStock() { return stock; }
    public Integer getReservedStock() { return reservedStock; }
    public Long getCategoryId() { return categoryId; }
    public String getImageUrl() { return imageUrl; }
    public Boolean getActive() { return active; }
    public Boolean getFlashSale() { return flashSale; }
    public LocalDateTime getFlashSaleStartTime() { return flashSaleStartTime; }
    public LocalDateTime getFlashSaleEndTime() { return flashSaleEndTime; }
    public Integer getFlashSaleStock() { return flashSaleStock; }
    public Integer getDiscountRate() { return discountRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Integer getAvailableStock() {
        return stock - reservedStock;
    }

    public boolean isFlashSaleActive() {
        if (!Boolean.TRUE.equals(flashSale)) return false;
        if (flashSaleStartTime == null || flashSaleEndTime == null) return false;

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(flashSaleStartTime) && now.isBefore(flashSaleEndTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%s, stock=%d, availableStock=%d}",
                id, name, price, stock, getAvailableStock());
    }
}