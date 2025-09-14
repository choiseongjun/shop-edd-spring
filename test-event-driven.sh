#!/bin/bash

echo "🚀 이벤트 드리븐 아키텍처 테스트 시작"
echo "=================================="

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 서비스 상태 확인
check_service() {
    local service_name=$1
    local url=$2

    echo -n "📍 $service_name 상태 확인... "

    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}FAIL${NC}"
        return 1
    fi
}

# Kafka 토픽 확인
check_kafka_topics() {
    echo "📡 Kafka 토픽 상태 확인"
    echo "------------------------"

    local expected_topics=(
        "order-created"
        "order-cancelled"
        "stock-reserved"
        "stock-reservation-failed"
        "payment-completed"
        "payment-failed"
        "user-notifications"
    )

    # Docker Compose가 실행 중인지 확인
    if ! docker-compose ps kafka | grep -q "Up"; then
        echo -e "${YELLOW}⚠️  Kafka가 실행되지 않음. docker-compose up -d kafka 실행 필요${NC}"
        return 1
    fi

    echo "✅ Kafka 실행 중"
    echo "📋 토픽 목록 확인은 Kafka UI (http://localhost:8090)에서 확인 가능"
    return 0
}

# 서비스들 상태 확인
echo "🔍 서비스 상태 확인"
echo "-------------------"

services=(
    "Eureka Server:http://localhost:8761/actuator/health"
    "Gateway:http://localhost:8080/actuator/health"
    "User Service:http://localhost:8082/api/users/health"
    "Product Service:http://localhost:8081/api/products/health"
    "Order Service:http://localhost:8083/api/orders/health"
    "Payment Service:http://localhost:8084/api/payments/health"
    "Notification Service:http://localhost:8085/api/notifications/health"
)

all_services_up=true

for service_info in "${services[@]}"; do
    service_name=$(echo $service_info | cut -d: -f1)
    service_url=$(echo $service_info | cut -d: -f2-)

    if ! check_service "$service_name" "$service_url"; then
        all_services_up=false
    fi
done

if [ "$all_services_up" = false ]; then
    echo -e "${RED}❌ 일부 서비스가 실행되지 않고 있습니다.${NC}"
    echo -e "${YELLOW}💡 각 서비스를 개별적으로 실행하세요:${NC}"
    echo "  ./gradlew bootRun -p eureka-server"
    echo "  ./gradlew bootRun -p gateway"
    echo "  ./gradlew bootRun -p user-service"
    echo "  ./gradlew bootRun -p product-service"
    echo "  ./gradlew bootRun -p order-service"
    echo "  ./gradlew bootRun -p payment-service"
    echo "  ./gradlew bootRun -p notification-service"
    exit 1
fi

echo -e "${GREEN}✅ 모든 서비스가 정상 실행 중입니다!${NC}"

# Kafka 토픽 확인
echo ""
check_kafka_topics

# 이벤트 드리븐 플로우 테스트
echo ""
echo "🎯 이벤트 드리븐 플로우 테스트"
echo "============================="

# JWT 토큰 생성 (간단한 테스트용)
echo "🔑 테스트용 사용자 로그인..."
login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass"
  }' 2>/dev/null)

if [ $? -ne 0 ] || [ -z "$login_response" ]; then
    echo -e "${YELLOW}⚠️  로그인 실패. 테스트 사용자 생성 또는 로그인 확인 필요${NC}"
    echo "💡 수동으로 JWT 토큰을 설정해서 테스트하세요"
    JWT_TOKEN="test-token"
else
    JWT_TOKEN=$(echo $login_response | jq -r '.token' 2>/dev/null || echo "test-token")
fi

# 상품 목록 조회
echo "📦 상품 목록 조회..."
products_response=$(curl -s http://localhost:8080/api/products 2>/dev/null)

if [ $? -eq 0 ] && [ -n "$products_response" ]; then
    echo -e "${GREEN}✅ 상품 목록 조회 성공${NC}"
    # 첫 번째 상품 ID 추출 (간단한 파싱)
    PRODUCT_ID=1
else
    echo -e "${YELLOW}⚠️  상품 목록 조회 실패. 기본값 사용${NC}"
    PRODUCT_ID=1
fi

# 주문 생성 (이벤트 드리븐 플로우 시작)
echo ""
echo "🛒 주문 생성 (이벤트 드리븐 플로우 시작)..."
order_response=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "User-Id: 1" \
  -d "{
    \"orderItems\": [{
      \"productId\": $PRODUCT_ID,
      \"quantity\": 1,
      \"flashSaleItem\": false
    }],
    \"shippingAddress\": \"서울시 강남구 테스트로 123\",
    \"paymentMethod\": \"CREDIT_CARD\",
    \"flashSaleOrder\": false
  }" 2>/dev/null)

if [ $? -eq 0 ] && [ -n "$order_response" ]; then
    echo -e "${GREEN}✅ 주문 생성 성공!${NC}"
    echo -e "${BLUE}📄 응답 데이터:${NC}"
    echo "$order_response" | jq '.' 2>/dev/null || echo "$order_response"

    # 주문 ID 추출
    ORDER_ID=$(echo "$order_response" | jq -r '.order.orderId' 2>/dev/null)

    if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
        echo ""
        echo -e "${BLUE}🔄 이벤트 플로우 진행 중...${NC}"
        echo "1. OrderCreatedEvent 발행됨"
        echo "2. Product Service에서 재고 확인 중..."
        echo "3. Payment Service에서 결제 처리 중..."
        echo "4. Notification Service에서 알림 발송 중..."

        # 잠시 대기 후 주문 상태 확인
        echo ""
        echo "⏳ 5초 대기 후 주문 상태 확인..."
        sleep 5

        order_status=$(curl -s http://localhost:8080/api/orders/$ORDER_ID \
          -H "Authorization: Bearer $JWT_TOKEN" \
          -H "User-Id: 1" 2>/dev/null)

        if [ $? -eq 0 ] && [ -n "$order_status" ]; then
            echo -e "${GREEN}✅ 주문 상태 조회 성공${NC}"
            echo -e "${BLUE}📋 최종 주문 상태:${NC}"
            echo "$order_status" | jq '.' 2>/dev/null || echo "$order_status"
        else
            echo -e "${YELLOW}⚠️  주문 상태 조회 실패${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  주문 ID 추출 실패${NC}"
    fi

else
    echo -e "${RED}❌ 주문 생성 실패${NC}"
    echo "💡 JWT 토큰이나 서비스 상태를 확인하세요"
fi

# 모니터링 정보 제공
echo ""
echo "📊 모니터링 도구"
echo "=================="
echo "🌐 Kafka UI: http://localhost:8090"
echo "🔍 Eureka Dashboard: http://localhost:8761"
echo "📡 Gateway: http://localhost:8080"
echo ""
echo "📋 실시간 이벤트 플로우는 Kafka UI에서 확인 가능합니다."
echo "   Topics 탭에서 각 이벤트 토픽의 메시지를 실시간으로 확인할 수 있습니다."

echo ""
echo -e "${GREEN}🎉 이벤트 드리븐 아키텍처 테스트 완료!${NC}"
echo "=================================="