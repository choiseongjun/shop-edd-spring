#!/bin/bash

# 분산 추적 테스트 스크립트
echo "🚀 Starting Distributed Tracing Test..."

# Zipkin UI 접속 정보
echo "📊 Zipkin UI: http://localhost:9411"
echo "🔍 서비스 추적을 위해 위 주소로 접속하세요"
echo ""

# 주문 생성 테스트
echo "📦 Testing Order Creation with Distributed Tracing..."

# 1. 상품 조회 (추적 시작점)
echo "🔍 1. Fetching product information..."
PRODUCT_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/products/1" \
  -H "Accept: application/json")

if [[ $? -eq 0 ]]; then
    echo "✅ Product fetch successful"
    echo "Response: $PRODUCT_RESPONSE"
else
    echo "❌ Product fetch failed"
    exit 1
fi

echo ""

# 2. 주문 생성 (주요 추적 대상)
echo "🛒 2. Creating order with tracing..."
ORDER_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -H "User-Id: 1" \
  -H "X-Trace-Id: test-order-$(date +%s)" \
  -d '{
    "orderItems": [
      {
        "productId": 1,
        "quantity": 2,
        "flashSaleItem": false
      }
    ],
    "shippingAddress": "서울시 강남구 테헤란로 123",
    "paymentMethod": "CREDIT_CARD",
    "flashSaleOrder": false
  }')

if [[ $? -eq 0 ]]; then
    echo "✅ Order creation successful"
    echo "Response: $ORDER_RESPONSE"

    # Extract order ID from response
    ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"orderId":"[^"]*' | cut -d'"' -f4)
    echo "📋 Order ID: $ORDER_ID"
else
    echo "❌ Order creation failed"
    exit 1
fi

echo ""

# 3. 주문 상태 확인
if [[ -n "$ORDER_ID" ]]; then
    echo "📋 3. Checking order status..."
    sleep 2  # 이벤트 처리 대기

    ORDER_STATUS=$(curl -s -X GET "http://localhost:8080/api/orders/user/1" \
      -H "Accept: application/json" \
      -H "User-Id: 1")

    if [[ $? -eq 0 ]]; then
        echo "✅ Order status check successful"
        echo "Response: $ORDER_STATUS"
    else
        echo "❌ Order status check failed"
    fi
fi

echo ""

# 4. 분산 추적 확인 안내
echo "🔍 Distributed Tracing Analysis:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Zipkin UI로 이동: http://localhost:9411"
echo "2. 'Find Traces' 버튼 클릭"
echo "3. Service Name에서 'order-service' 선택"
echo "4. 최근 traces를 확인하여 다음 정보를 볼 수 있습니다:"
echo ""
echo "   📈 예상되는 추적 스팬들:"
echo "   ├── order.create (전체 주문 생성 프로세스)"
echo "   ├── order.validation (주문 검증)"
echo "   ├── order.items.processing (주문 아이템 처리)"
echo "   │   └── product-service.get-product (상품 정보 조회)"
echo "   ├── order.save (주문 저장)"
echo "   └── order.event.publish (주문 생성 이벤트 발행)"
echo ""
echo "   🏷️  태그 정보:"
echo "   ├── user.id: 사용자 ID"
echo "   ├── order.id: 주문 ID"
echo "   ├── product.id: 상품 ID"
echo "   ├── order.items.count: 주문 아이템 수"
echo "   └── order.total.amount: 주문 총액"
echo ""

# 5. 추가 테스트 제안
echo "🧪 Additional Testing Suggestions:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. 여러 주문을 동시에 생성하여 동시성 추적 확인"
echo "2. 의도적으로 실패하는 주문을 만들어 에러 추적 확인"
echo "3. 다양한 서비스 간 호출 패턴 확인"
echo ""

# 6. 서비스 상태 확인
echo "🏥 Service Health Check:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
SERVICES=("order-service:8083" "product-service:8081" "payment-service:8084" "notification-service:8085")

for service in "${SERVICES[@]}"; do
    service_name=$(echo $service | cut -d':' -f1)
    port=$(echo $service | cut -d':' -f2)

    health_response=$(curl -s "http://localhost:$port/actuator/health" 2>/dev/null)
    if [[ $? -eq 0 ]] && [[ $health_response == *"UP"* ]]; then
        echo "✅ $service_name is UP"
    else
        echo "❌ $service_name is DOWN or not responding"
    fi
done

echo ""
echo "🎉 Distributed Tracing Test Complete!"
echo "📊 Visit Zipkin UI to explore the traces: http://localhost:9411"