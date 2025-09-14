#!/bin/bash

# 플래시 세일 MSA 빠른 테스트 스크립트

echo "🚀 플래시 세일 MSA 시스템 테스트 시작..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
test_service() {
    local service_name=$1
    local url=$2
    echo -e "${BLUE}Testing $service_name...${NC}"
    
    response=$(curl -s -w "%{http_code}" $url)
    http_code="${response: -3}"
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✅ $service_name: OK${NC}"
    else
        echo -e "${RED}❌ $service_name: FAILED (HTTP $http_code)${NC}"
    fi
}

echo -e "${YELLOW}📋 1. Health Check 테스트${NC}"
test_service "Eureka Server" "http://localhost:8761"
test_service "API Gateway" "http://localhost:8080/actuator/health"
test_service "User Service" "http://localhost:8080/api/users/health"
test_service "Product Service" "http://localhost:8080/api/products/health"

echo ""
echo -e "${YELLOW}👤 2. 사용자 서비스 테스트${NC}"

# 회원가입
echo -e "${BLUE}회원가입 테스트...${NC}"
register_response=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "test123@example.com", 
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }')

if [[ $register_response == *"successfully"* ]]; then
    echo -e "${GREEN}✅ 회원가입: 성공${NC}"
else
    echo -e "${RED}❌ 회원가입: 실패${NC}"
fi

# 로그인
echo -e "${BLUE}로그인 테스트...${NC}"
login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser123",
    "password": "password123"
  }')

if [[ $login_response == *"token"* ]]; then
    echo -e "${GREEN}✅ 로그인: 성공${NC}"
    TOKEN=$(echo $login_response | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo "JWT Token: ${TOKEN:0:50}..."
else
    echo -e "${RED}❌ 로그인: 실패${NC}"
    echo "Response: $login_response"
fi

echo ""
echo -e "${YELLOW}🛍️ 3. 상품 서비스 테스트${NC}"

# 상품 생성
echo -e "${BLUE}플래시 세일 상품 생성...${NC}"
product_response=$(curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 플래시세일 상품",
    "description": "테스트용 상품입니다",
    "price": 50000,
    "originalPrice": 100000,
    "stock": 100,
    "categoryId": 1,
    "active": true,
    "flashSale": true,
    "flashSaleStartTime": "2024-01-01T10:00:00",
    "flashSaleEndTime": "2024-12-31T23:59:59", 
    "flashSaleStock": 100,
    "discountRate": 50
  }')

if [[ $product_response == *"id"* ]]; then
    echo -e "${GREEN}✅ 상품 생성: 성공${NC}"
    PRODUCT_ID=$(echo $product_response | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo "생성된 상품 ID: $PRODUCT_ID"
else
    echo -e "${RED}❌ 상품 생성: 실패${NC}"
    echo "Response: $product_response"
    PRODUCT_ID=1  # 기본값 설정
fi

# 상품 조회
echo -e "${BLUE}상품 조회 테스트...${NC}"
get_product_response=$(curl -s http://localhost:8080/api/products/$PRODUCT_ID)

if [[ $get_product_response == *"name"* ]]; then
    echo -e "${GREEN}✅ 상품 조회: 성공${NC}"
else
    echo -e "${RED}❌ 상품 조회: 실패${NC}"
fi

# 플래시 세일 상품 조회
echo -e "${BLUE}플래시 세일 상품 조회...${NC}"
flash_sale_response=$(curl -s http://localhost:8080/api/products/flash-sale)

if [[ $flash_sale_response == *"["* ]]; then
    echo -e "${GREEN}✅ 플래시 세일 상품 조회: 성공${NC}"
else
    echo -e "${RED}❌ 플래시 세일 상품 조회: 실패${NC}"
fi

echo ""
echo -e "${YELLOW}📦 4. 재고 예약 테스트${NC}"

# 재고 예약
echo -e "${BLUE}재고 예약 테스트...${NC}"
reserve_response=$(curl -s -X POST http://localhost:8080/api/products/reserve-stock \
  -H "Content-Type: application/json" \
  -d "{
    \"productId\": $PRODUCT_ID,
    \"quantity\": 1,
    \"userId\": 1,
    \"orderId\": \"TEST_ORDER_$(date +%s)\"
  }")

if [[ $reserve_response == *"success"* ]]; then
    echo -e "${GREEN}✅ 재고 예약: 성공${NC}"
else
    echo -e "${RED}❌ 재고 예약: 실패${NC}"
    echo "Response: $reserve_response"
fi

echo ""
echo -e "${YELLOW}🔥 5. 동시성 테스트 (간단)${NC}"

echo -e "${BLUE}동시 재고 예약 테스트 (10개 요청)...${NC}"
success_count=0

for i in {1..10}; do
    response=$(curl -s -X POST http://localhost:8080/api/products/reserve-stock \
      -H "Content-Type: application/json" \
      -d "{
        \"productId\": $PRODUCT_ID,
        \"quantity\": 1,
        \"userId\": $i,
        \"orderId\": \"CONCURRENT_TEST_$i\"
      }" &)
    
    if [[ $response == *"success"* ]]; then
        ((success_count++))
    fi
done

wait
echo -e "${GREEN}동시 요청 중 성공한 예약: $success_count/10${NC}"

echo ""
echo -e "${YELLOW}📊 6. 시스템 상태 확인${NC}"

# Eureka 등록된 서비스 확인
echo -e "${BLUE}Eureka 등록 서비스 확인...${NC}"
eureka_response=$(curl -s http://localhost:8761/eureka/apps -H "Accept: application/json")

if [[ $eureka_response == *"application"* ]]; then
    echo -e "${GREEN}✅ Eureka 서비스 등록: 정상${NC}"
    # 등록된 서비스 수 계산
    service_count=$(echo $eureka_response | grep -o '"name"' | wc -l)
    echo "등록된 서비스 수: $service_count"
else
    echo -e "${RED}❌ Eureka 서비스 등록: 확인 불가${NC}"
fi

# Gateway 메트릭 확인
echo -e "${BLUE}Gateway 메트릭 확인...${NC}"
metrics_response=$(curl -s http://localhost:8080/actuator/metrics)

if [[ $metrics_response == *"names"* ]]; then
    echo -e "${GREEN}✅ Gateway 메트릭: 정상${NC}"
else
    echo -e "${RED}❌ Gateway 메트릭: 확인 불가${NC}"
fi

echo ""
echo -e "${YELLOW}🎯 7. Rate Limiting 테스트${NC}"

echo -e "${BLUE}Rate Limiting 테스트 (20개 빠른 요청)...${NC}"
rate_limit_count=0

for i in {1..20}; do
    http_code=$(curl -s -w "%{http_code}" http://localhost:8080/api/products -o /dev/null)
    if [ "$http_code" -eq 429 ]; then
        ((rate_limit_count++))
    fi
done

if [ $rate_limit_count -gt 0 ]; then
    echo -e "${GREEN}✅ Rate Limiting 동작: $rate_limit_count/20 요청이 제한됨${NC}"
else
    echo -e "${YELLOW}⚠️ Rate Limiting: 제한이 적용되지 않음 (설정 확인 필요)${NC}"
fi

echo ""
echo -e "${GREEN}🎉 테스트 완료!${NC}"
echo ""
echo -e "${BLUE}📋 추가 테스트를 위한 유용한 URL:${NC}"
echo "- Eureka Dashboard: http://localhost:8761"
echo "- Kafka UI: http://localhost:8090"
echo "- Gateway Health: http://localhost:8080/actuator/health"
echo ""
echo -e "${BLUE}💡 JWT 토큰이 발급되었다면 다음 명령어로 프로필 조회 가능:${NC}"
echo "curl -H \"Authorization: Bearer $TOKEN\" http://localhost:8080/api/users/profile"
echo ""
echo -e "${BLUE}🔍 상세한 API 테스트는 api-test-commands.md 파일을 참고하세요.${NC}"