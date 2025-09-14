#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== 상세 재고 진단 ==="

# 1. 상품 목록 조회 (첫 3개만)
echo "1. 상품 목록 (첫 3개):"
curl -s -X GET "${BASE_URL}/api/products" | jq '.[0:3] | .[] | {id, name, stock, reservedStock, availableStock: (.stock - (.reservedStock // 0)), flashSale, flashSaleActive}'

# 2. 첫 번째 상품 상세 조회
echo -e "\n2. 첫 번째 상품 상세 정보:"
curl -s -X GET "${BASE_URL}/api/products/1" | jq '.'

# 3. 재고 예약 시도 (수량 1)
echo -e "\n3. 재고 예약 시도 (수량 1):"
curl -s -X POST "${BASE_URL}/api/products/reserve-stock" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "userId": 1,
    "orderId": "DEBUG_001"
  }' | jq '.'

# 4. 다시 상품 조회해서 변경사항 확인
echo -e "\n4. 예약 후 상품 상태:"
curl -s -X GET "${BASE_URL}/api/products/1" | jq '{id, name, stock, reservedStock, availableStock: (.stock - (.reservedStock // 0))}'

echo -e "\n=== 진단 완료 ==="
