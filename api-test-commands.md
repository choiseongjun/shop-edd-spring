# Distributed Shop API Test Commands

## 기본 설정
```bash
BASE_URL="http://localhost:8080"
```

## 1. Gateway 헬스체크
```bash
curl -X GET "${BASE_URL}/actuator/health" \
  -H "Content-Type: application/json"
```

## 2. User Service APIs

### 2.1 사용자 등록
```bash
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "010-1234-5678",
    "address": "서울시 강남구"
  }'
```

### 2.2 사용자 로그인
```bash
curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

### 2.3 사용자 프로필 조회
```bash
curl -X GET "${BASE_URL}/api/users/profile" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 2.4 사용자 정보 수정
```bash
curl -X PUT "${BASE_URL}/api/users/profile" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "phoneNumber": "010-9876-5432",
    "address": "서울시 서초구"
  }'
```

### 2.5 User Service 헬스체크
```bash
curl -X GET "${BASE_URL}/api/users/health" \
  -H "Content-Type: application/json"
```

## 3. Product Service APIs

### 3.1 모든 상품 조회
```bash
curl -X GET "${BASE_URL}/api/products" \
  -H "Content-Type: application/json"
```

### 3.2 특정 상품 조회
```bash
curl -X GET "${BASE_URL}/api/products/1" \
  -H "Content-Type: application/json"
```

### 3.3 플래시 세일 상품 조회
```bash
curl -X GET "${BASE_URL}/api/products/flash-sale" \
  -H "Content-Type: application/json"
```

### 3.4 카테고리별 상품 조회
```bash
curl -X GET "${BASE_URL}/api/products/category/1" \
  -H "Content-Type: application/json"
```

### 3.5 상품 검색
```bash
curl -X GET "${BASE_URL}/api/products/search?keyword=스마트폰" \
  -H "Content-Type: application/json"
```

### 3.6 상품 생성 (관리자)
```bash
curl -X POST "${BASE_URL}/api/products" \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "새로운 상품",
    "description": "상품 설명",
    "price": 100000,
    "originalPrice": 150000,
    "stock": 100,
    "categoryId": 1,
    "imageUrl": "https://example.com/product.jpg",
    "active": true,
    "flashSale": false
  }'
```

### 3.7 플래시 세일 상품 생성
```bash
curl -X POST "${BASE_URL}/api/products" \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "한정판 스마트폰",
    "description": "1000개 한정! 70% 할인",
    "price": 300000,
    "originalPrice": 1000000,
    "stock": 1000,
    "categoryId": 1,
    "imageUrl": "https://example.com/phone.jpg",
    "active": true,
    "flashSale": true,
    "flashSaleStartTime": "2024-01-01T10:00:00",
    "flashSaleEndTime": "2024-01-01T12:00:00",
    "flashSaleStock": 1000,
    "discountRate": 70
  }'
```

### 3.8 상품 수정
```bash
curl -X PUT "${BASE_URL}/api/products/1" \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "수정된 상품명",
    "description": "수정된 설명",
    "price": 120000,
    "stock": 150
  }'
```

### 3.9 재고 예약
```bash
curl -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "userId": 1,
    "orderId": "ORDER_123"
  }'
```

### 3.10 재고 확정
```bash
curl -X POST "${BASE_URL}/api/products/confirm-stock-reduction" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_123",
    "productId": 1,
    "quantity": 2
  }'
```

### 3.11 예약 재고 해제
```bash
curl -X POST "${BASE_URL}/api/products/release-reserved-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_123",
    "productId": 1,
    "quantity": 2
  }'
```

### 3.12 Product Service 헬스체크
```bash
curl -X GET "${BASE_URL}/api/products/health" \
  -H "Content-Type: application/json"
```

## 4. Order Service APIs

### 4.1 주문 생성
```bash
curl -X POST "${BASE_URL}/api/orders" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "orderItems": [
      {
        "productId": 1,
        "quantity": 2,
        "unitPrice": 100000,
        "flashSaleItem": false
      }
    ],
    "shippingAddress": "서울시 강남구 테헤란로 123",
    "paymentMethod": "CREDIT_CARD",
    "flashSaleOrder": false
  }'
```

### 4.2 플래시 세일 주문 생성
```bash
curl -X POST "${BASE_URL}/api/orders" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "orderItems": [
      {
        "productId": 2,
        "quantity": 1,
        "unitPrice": 300000,
        "flashSaleItem": true
      }
    ],
    "shippingAddress": "서울시 강남구 테헤란로 123",
    "paymentMethod": "CREDIT_CARD",
    "flashSaleOrder": true
  }'
```

### 4.3 사용자 주문 목록 조회
```bash
curl -X GET "${BASE_URL}/api/orders" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.4 특정 주문 조회
```bash
curl -X GET "${BASE_URL}/api/orders/ORDER_ID_HERE" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.5 주문 상태 수정
```bash
curl -X PUT "${BASE_URL}/api/orders/ORDER_ID_HERE/status?status=CONFIRMED" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.6 주문 취소
```bash
curl -X DELETE "${BASE_URL}/api/orders/ORDER_ID_HERE" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.7 주문 확정
```bash
curl -X POST "${BASE_URL}/api/orders/ORDER_ID_HERE/confirm" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.8 플래시 세일 주문 조회
```bash
curl -X GET "${BASE_URL}/api/orders/flash-sale" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.9 Order Service 헬스체크
```bash
curl -X GET "${BASE_URL}/api/orders/health" \
  -H "Content-Type: application/json"
```

## 5. Payment Service APIs

### 5.1 결제 처리
```bash
curl -X POST "${BASE_URL}/api/payments/process" \
  -H "User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_ID_HERE",
    "amount": 200000,
    "paymentMethod": "CREDIT_CARD",
    "flashSalePayment": false
  }'
```

### 5.2 플래시 세일 결제 처리
```bash
curl -X POST "${BASE_URL}/api/payments/process" \
  -H "User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_ID_HERE",
    "amount": 300000,
    "paymentMethod": "CREDIT_CARD",
    "flashSalePayment": true
  }'
```

### 5.3 결제 내역 조회
```bash
curl -X GET "${BASE_URL}/api/payments/history" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 5.4 특정 결제 조회
```bash
curl -X GET "${BASE_URL}/api/payments/PAYMENT_ID_HERE" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 5.5 결제 취소
```bash
curl -X POST "${BASE_URL}/api/payments/PAYMENT_ID_HERE/cancel" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 5.6 결제 환불
```bash
curl -X POST "${BASE_URL}/api/payments/PAYMENT_ID_HERE/refund" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "refundAmount": 100000,
    "reason": "고객 요청"
  }'
```

### 5.7 Payment Service 헬스체크
```bash
curl -X GET "${BASE_URL}/api/payments/health" \
  -H "Content-Type: application/json"
```

## 6. Notification Service APIs

### 6.1 알림 전송
```bash
curl -X POST "${BASE_URL}/api/notifications/send" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "type": "ORDER_CONFIRMED",
    "title": "주문 확인",
    "message": "주문이 확인되었습니다.",
    "data": {
      "orderId": "ORDER_123"
    }
  }'
```

### 6.2 사용자 알림 조회
```bash
curl -X GET "${BASE_URL}/api/notifications/user/1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 6.3 알림 읽음 처리
```bash
curl -X PUT "${BASE_URL}/api/notifications/NOTIFICATION_ID_HERE/read" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 6.4 알림 삭제
```bash
curl -X DELETE "${BASE_URL}/api/notifications/NOTIFICATION_ID_HERE" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 6.5 Notification Service 헬스체크
```bash
curl -X GET "${BASE_URL}/api/notifications/health" \
  -H "Content-Type: application/json"
```

## 7. Eureka Server APIs

### 7.1 Eureka Server 헬스체크
```bash
curl -X GET "http://localhost:8761/actuator/health" \
  -H "Content-Type: application/json"
```

### 7.2 등록된 서비스 목록
```bash
curl -X GET "http://localhost:8761/eureka/apps" \
  -H "Content-Type: application/json"
```

## 8. 테스트 시나리오

### 8.1 전체 플로우 테스트
```bash
# 1. 사용자 등록
USER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "010-1234-5678",
    "address": "서울시 강남구"
  }')

echo "User registration: $USER_RESPONSE"

# 2. 로그인
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
echo "Login token: $TOKEN"

# 3. 상품 목록 조회
PRODUCTS_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/products")
echo "Products: $PRODUCTS_RESPONSE"

# 4. 주문 생성
ORDER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/orders" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderItems": [
      {
        "productId": 1,
        "quantity": 1,
        "unitPrice": 100000,
        "flashSaleItem": false
      }
    ],
    "shippingAddress": "서울시 강남구 테헤란로 123",
    "paymentMethod": "CREDIT_CARD",
    "flashSaleOrder": false
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.order.orderId')
echo "Order created: $ORDER_ID"

# 5. 결제 처리
PAYMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/payments/process" \
  -H "User-Id: 1" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"amount\": 100000,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"flashSalePayment\": false
  }")

echo "Payment processed: $PAYMENT_RESPONSE"
```

### 8.2 플래시 세일 테스트
```bash
# 1. 플래시 세일 상품 생성 (관리자)
FLASH_PRODUCT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/products" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "한정판 스마트폰",
    "description": "1000개 한정! 70% 할인",
    "price": 300000,
    "originalPrice": 1000000,
    "stock": 1000,
    "categoryId": 1,
    "imageUrl": "https://example.com/phone.jpg",
    "active": true,
    "flashSale": true,
    "flashSaleStartTime": "2024-01-01T10:00:00",
    "flashSaleEndTime": "2024-01-01T12:00:00",
    "flashSaleStock": 1000,
    "discountRate": 70
  }')

FLASH_PRODUCT_ID=$(echo $FLASH_PRODUCT_RESPONSE | jq -r '.id')
echo "Flash sale product created: $FLASH_PRODUCT_ID"

# 2. 플래시 세일 주문 생성
FLASH_ORDER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/orders" \
  -H "User-Id: 1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderItems\": [
      {
        \"productId\": $FLASH_PRODUCT_ID,
        \"quantity\": 1,
        \"unitPrice\": 300000,
        \"flashSaleItem\": true
      }
    ],
    \"shippingAddress\": \"서울시 강남구 테헤란로 123\",
    \"paymentMethod\": \"CREDIT_CARD\",
    \"flashSaleOrder\": true
  }")

FLASH_ORDER_ID=$(echo $FLASH_ORDER_RESPONSE | jq -r '.order.orderId')
echo "Flash sale order created: $FLASH_ORDER_ID"

# 3. 플래시 세일 결제 처리
FLASH_PAYMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/payments/process" \
  -H "User-Id: 1" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$FLASH_ORDER_ID\",
    \"amount\": 300000,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"flashSalePayment\": true
  }")

echo "Flash sale payment processed: $FLASH_PAYMENT_RESPONSE"
```

## 9. 헬스체크 스크립트

### 9.1 전체 서비스 헬스체크
```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== 서비스 헬스체크 시작 ==="

# Gateway
echo "Gateway: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health")"

# User Service
echo "User Service: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/users/health")"

# Product Service
echo "Product Service: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/products/health")"

# Order Service
echo "Order Service: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/orders/health")"

# Payment Service
echo "Payment Service: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/payments/health")"

# Notification Service
echo "Notification Service: $(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/notifications/health")"

# Eureka Server
echo "Eureka Server: $(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8761/actuator/health")"

echo "=== 서비스 헬스체크 완료 ==="
```

## 10. 주의사항

1. **토큰 교체**: `YOUR_TOKEN_HERE`, `ADMIN_TOKEN_HERE` 등을 실제 토큰으로 교체하세요.
2. **ID 교체**: `ORDER_ID_HERE`, `PAYMENT_ID_HERE` 등을 실제 ID로 교체하세요.
3. **서비스 실행**: 모든 서비스가 실행 중인지 확인하세요.
4. **데이터베이스**: PostgreSQL과 Redis가 실행 중인지 확인하세요.
5. **포트**: 모든 서비스가 올바른 포트에서 실행 중인지 확인하세요.

## 11. 에러 해결

### 11.1 연결 거부 에러
```bash
# 서비스 상태 확인
docker ps
# 또는
netstat -tulpn | grep :8080
```

### 11.2 인증 에러
```bash
# 토큰 유효성 확인
curl -X GET "${BASE_URL}/api/users/profile" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 11.3 데이터베이스 연결 에러
```bash
# PostgreSQL 연결 확인
psql -h localhost -p 5432 -U shop_user -d product_db

# Redis 연결 확인
redis-cli ping
```