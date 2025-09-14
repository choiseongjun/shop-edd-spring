#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== 빠른 재고 진단 ==="

# 1. 상품 목록 조회
echo "상품 목록:"
curl -s -X GET "${BASE_URL}/api/products" | jq '.[0:3]' 2>/dev/null || echo "상품 목록 조회 실패"

# 2. 첫 번째 상품 상세 조회
echo -e "\n첫 번째 상품 상세:"
curl -s -X GET "${BASE_URL}/api/products/1" | jq '.' 2>/dev/null || echo "상품 상세 조회 실패"

# 3. 재고 예약 시도 (수량 1)
echo -e "\n재고 예약 시도 (수량 1):"
curl -s -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "userId": 1,
    "orderId": "DEBUG_001"
  }' | jq '.' 2>/dev/null || echo "재고 예약 실패"

echo -e "\n=== 진단 완료 ==="
