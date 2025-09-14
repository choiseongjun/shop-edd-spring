package com.jun.orderservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "flash_sale_item")
    private Boolean flashSaleItem = false;

    protected OrderItem() {}

    private OrderItem(Builder builder) {
        this.order = builder.order;
        this.productId = builder.productId;
        this.productName = builder.productName;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.flashSaleItem = builder.flashSaleItem;
        this.totalPrice = calculateTotalPrice();
    }

    private BigDecimal calculateTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Order order;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Boolean flashSaleItem = false;

        public Builder order(Order order) {
            this.order = order;
            return this;
        }

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder flashSaleItem(Boolean flashSaleItem) {
            this.flashSaleItem = flashSaleItem;
            return this;
        }

        public OrderItem build() {
            validateRequiredFields();
            return new OrderItem(this);
        }

        private void validateRequiredFields() {
            Objects.requireNonNull(order, "Order cannot be null");
            Objects.requireNonNull(productId, "Product ID cannot be null");
            Objects.requireNonNull(productName, "Product name cannot be null");
            Objects.requireNonNull(quantity, "Quantity cannot be null");
            Objects.requireNonNull(unitPrice, "Unit price cannot be null");

            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
        }
    }

    // Getters only (immutable after creation)
    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public Boolean getFlashSaleItem() { return flashSaleItem; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("OrderItem{id=%d, productId=%d, productName='%s', quantity=%d, unitPrice=%s, totalPrice=%s}",
                id, productId, productName, quantity, unitPrice, totalPrice);
    }
}