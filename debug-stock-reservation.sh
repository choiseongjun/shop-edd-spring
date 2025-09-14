#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== 재고 예약 문제 진단 ==="

# 1. 상품 목록 조회
echo "1. 상품 목록 조회:"
curl -s -X GET "${BASE_URL}/api/products" | jq '.'

echo -e "\n2. 특정 상품 상세 조회 (ID: 1):"
curl -s -X GET "${BASE_URL}/api/products/1" | jq '.'

echo -e "\n3. 플래시 세일 상품 조회:"
curl -s -X GET "${BASE_URL}/api/products/flash-sale" | jq '.'

echo -e "\n4. 재고 예약 시도 (ID: 1, 수량: 1):"
curl -s -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "userId": 1,
    "orderId": "TEST_ORDER_001"
  }' | jq '.'

echo -e "\n5. 재고 예약 시도 (ID: 1, 수량: 10):"
curl -s -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 10,
    "userId": 1,
    "orderId": "TEST_ORDER_002"
  }' | jq '.'

echo -e "\n6. 존재하지 않는 상품 재고 예약 시도 (ID: 999):"
curl -s -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 999,
    "quantity": 1,
    "userId": 1,
    "orderId": "TEST_ORDER_003"
  }' | jq '.'

echo -e "\n=== 진단 완료 ==="
