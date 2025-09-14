package com.jun.productservice.service;

import com.jun.productservice.dto.ProductDto;
import com.jun.productservice.dto.StockReservationRequest;
import com.jun.productservice.entity.Product;
import com.jun.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final String FLASH_SALE_CACHE_KEY = "flash_sale_products";
    private static final long CACHE_TTL = 300; // 5분
    
    @Cacheable(value = "products", key = "#id")
    public ProductDto getProduct(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(this::convertToDto).orElse(null);
    }
    
    @Cacheable(value = "products", key = "'all_active'")
    public List<ProductDto> getAllActiveProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "flash_sale_products", key = "'active'")
    public List<ProductDto> getActiveFlashSaleProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = productRepository.findActiveFlashSaleProducts(now);
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ProductDto> searchProducts(String keyword) {
        List<Product> products = productRepository.findByKeyword(keyword);
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = {"products", "flash_sale_products"}, allEntries = true)
    public ProductDto createProduct(ProductDto productDto) {
        Product product = convertToEntity(productDto);
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }
    
    @Transactional
    @CacheEvict(value = {"products", "flash_sale_products"}, allEntries = true)
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        return productRepository.findById(id)
                .map(product -> {
                    updateProductFields(product, productDto);
                    Product savedProduct = productRepository.save(product);
                    return convertToDto(savedProduct);
                })
                .orElse(null);
    }

    private void updateProductFields(Product product, ProductDto productDto) {
        if (productDto.getName() != null) {
            product.setName(productDto.getName());
        }
        if (productDto.getDescription() != null) {
            product.setDescription(productDto.getDescription());
        }
        if (productDto.getPrice() != null) {
            product.updatePrice(productDto.getPrice());
        }
        if (productDto.getOriginalPrice() != null) {
            product.setOriginalPrice(productDto.getOriginalPrice());
        }
        if (productDto.getStock() != null) {
            product.updateStock(productDto.getStock());
        }
        if (productDto.getCategoryId() != null) {
            product.setCategoryId(productDto.getCategoryId());
        }
        if (productDto.getImageUrl() != null) {
            product.setImageUrl(productDto.getImageUrl());
        }
        if (productDto.getActive() != null) {
            if (productDto.getActive()) {
                product.activate();
            } else {
                product.deactivate();
            }
        }
        if (productDto.getFlashSale() != null) {
            product.setFlashSale(productDto.getFlashSale());
        }
        if (productDto.getFlashSaleStartTime() != null) {
            product.setFlashSaleStartTime(productDto.getFlashSaleStartTime());
        }
        if (productDto.getFlashSaleEndTime() != null) {
            product.setFlashSaleEndTime(productDto.getFlashSaleEndTime());
        }
        if (productDto.getFlashSaleStock() != null) {
            product.setFlashSaleStock(productDto.getFlashSaleStock());
        }
        if (productDto.getDiscountRate() != null) {
            product.setDiscountRate(productDto.getDiscountRate());
        }
    }
    
    @Transactional
    public boolean reserveStock(StockReservationRequest request) {
        String lockKey = "stock_lock:" + request.getProductId();
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 5, TimeUnit.SECONDS);
        
        if (!lockAcquired) {
            return false;
        }
        
        try {
            Optional<Product> productOpt = productRepository.findByIdWithLock(request.getProductId());
            if (productOpt.isEmpty()) {
                return false;
            }
            
            Product product = productOpt.get();
            
            // 플래시 세일 상품인 경우 추가 검증
            if (product.getFlashSale() && !product.isFlashSaleActive()) {
                return false;
            }
            
            if (!product.hasAvailableStock(request.getQuantity())) {
                return false;
            }

            try {
                product.reserveStock(request.getQuantity());
                productRepository.save(product);

                String reservationKey = "reservation:" + request.getOrderId();
                redisTemplate.opsForHash().put(reservationKey, "productId", request.getProductId());
                redisTemplate.opsForHash().put(reservationKey, "quantity", request.getQuantity());
                redisTemplate.opsForHash().put(reservationKey, "userId", request.getUserId());
                redisTemplate.expire(reservationKey, 10, TimeUnit.MINUTES);

                evictProductCache(request.getProductId());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    @Transactional
    public boolean confirmStockReduction(String orderId, Long productId, Integer quantity) {
        String reservationKey = "reservation:" + orderId;
        Object reservedProductId = redisTemplate.opsForHash().get(reservationKey, "productId");
        Object reservedQuantity = redisTemplate.opsForHash().get(reservationKey, "quantity");
        
        if (reservedProductId == null || !reservedProductId.equals(productId) ||
            reservedQuantity == null || !reservedQuantity.equals(quantity)) {
            return false;
        }
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            try {
                product.confirmStockReduction(quantity);
                productRepository.save(product);
                redisTemplate.delete(reservationKey);
                evictProductCache(productId);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }
    
    @Transactional
    public boolean releaseReservedStock(String orderId, Long productId, Integer quantity) {
        String reservationKey = "reservation:" + orderId;
        Object reservedProductId = redisTemplate.opsForHash().get(reservationKey, "productId");
        Object reservedQuantity = redisTemplate.opsForHash().get(reservationKey, "quantity");

        if (reservedProductId == null || !reservedProductId.equals(productId) ||
            reservedQuantity == null || !reservedQuantity.equals(quantity)) {
            return false;
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            try {
                product.releaseReservedStock(quantity);
                productRepository.save(product);
                redisTemplate.delete(reservationKey);
                evictProductCache(productId);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }

    @Transactional
    public boolean reserveStock(Long productId, Integer quantity, String orderId) {
        String lockKey = "stock_lock:" + productId;
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 5, TimeUnit.SECONDS);

        if (!lockAcquired) {
            return false;
        }

        try {
            Optional<Product> productOpt = productRepository.findByIdWithLock(productId);
            if (productOpt.isEmpty()) {
                return false;
            }

            Product product = productOpt.get();

            if (product.getFlashSale() && !product.isFlashSaleActive()) {
                return false;
            }

            if (!product.hasAvailableStock(quantity)) {
                return false;
            }

            try {
                product.reserveStock(quantity);
                productRepository.save(product);

                String reservationKey = "reservation:" + orderId;
                redisTemplate.opsForHash().put(reservationKey, "productId", productId);
                redisTemplate.opsForHash().put(reservationKey, "quantity", quantity);
                redisTemplate.expire(reservationKey, 10, TimeUnit.MINUTES);

                evictProductCache(productId);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional
    public boolean releaseReservedStock(Long productId, Integer quantity, String orderId) {
        String reservationKey = "reservation:" + orderId;
        Object reservedProductId = redisTemplate.opsForHash().get(reservationKey, "productId");
        Object reservedQuantity = redisTemplate.opsForHash().get(reservationKey, "quantity");

        if (reservedProductId != null && reservedProductId.equals(productId) &&
            reservedQuantity != null && reservedQuantity.equals(quantity)) {

            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                try {
                    product.releaseReservedStock(quantity);
                    productRepository.save(product);
                    redisTemplate.delete(reservationKey);
                    evictProductCache(productId);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        return false;
    }
    
    private void evictProductCache(Long productId) {
        redisTemplate.delete(PRODUCT_CACHE_KEY + productId);
        redisTemplate.delete(FLASH_SALE_CACHE_KEY);
    }
    
    private ProductDto convertToDto(Product product) {
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getOriginalPrice(),
            product.getStock(),
            product.getAvailableStock(),
            product.getCategoryId(),
            product.getImageUrl(),
            product.getActive(),
            product.getFlashSale(),
            product.getFlashSaleStartTime(),
            product.getFlashSaleEndTime(),
            product.getFlashSaleStock(),
            product.getDiscountRate(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.isFlashSaleActive()
        );
    }
    
    private Product convertToEntity(ProductDto dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .stock(dto.getStock())
                .categoryId(dto.getCategoryId())
                .imageUrl(dto.getImageUrl())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .flashSale(dto.getFlashSale() != null ? dto.getFlashSale() : false)
                .flashSaleStartTime(dto.getFlashSaleStartTime())
                .flashSaleEndTime(dto.getFlashSaleEndTime())
                .flashSaleStock(dto.getFlashSaleStock())
                .discountRate(dto.getDiscountRate() != null ? dto.getDiscountRate() : 0)
                .build();
    }
}