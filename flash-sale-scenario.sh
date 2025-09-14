#!/bin/bash

# 플래시 세일 시나리오 테스트 스크립트

echo "🔥 플래시 세일 시나리오 테스트 시작..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 설정
CONCURRENT_USERS=50
FLASH_SALE_STOCK=100

echo -e "${PURPLE}📋 시나리오: $CONCURRENT_USERS명이 동시에 $FLASH_SALE_STOCK개 한정 상품 구매 시도${NC}"
echo ""

# 1단계: 플래시 세일 상품 생성
echo -e "${YELLOW}🛍️ 1단계: 플래시 세일 상품 생성${NC}"

product_response=$(curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"⚡ 한정판 울트라북 ⚡\",
    \"description\": \"${FLASH_SALE_STOCK}개 한정! 선착순 80% 할인!\",
    \"price\": 200000,
    \"originalPrice\": 1000000,
    \"stock\": $FLASH_SALE_STOCK,
    \"categoryId\": 1,
    \"imageUrl\": \"https://example.com/ultrabook.jpg\",
    \"active\": true,
    \"flashSale\": true,
    \"flashSaleStartTime\": \"2024-01-01T10:00:00\",
    \"flashSaleEndTime\": \"2024-12-31T23:59:59\",
    \"flashSaleStock\": $FLASH_SALE_STOCK,
    \"discountRate\": 80
  }")

if [[ $product_response == *"id"* ]]; then
    PRODUCT_ID=$(echo $product_response | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✅ 플래시 세일 상품 생성 완료! (상품 ID: $PRODUCT_ID)${NC}"
    echo -e "${BLUE}   - 상품명: ⚡ 한정판 울트라북 ⚡${NC}"
    echo -e "${BLUE}   - 정가: 1,000,000원 → 할인가: 200,000원 (80% 할인)${NC}"
    echo -e "${BLUE}   - 한정 수량: $FLASH_SALE_STOCK개${NC}"
else
    echo -e "${RED}❌ 플래시 세일 상품 생성 실패${NC}"
    echo "Response: $product_response"
    exit 1
fi

echo ""

# 2단계: 다중 사용자 생성
echo -e "${YELLOW}👥 2단계: 플래시 세일 참여자 생성 (최대 20명)${NC}"

USER_COUNT=20
TOKENS=()

for i in $(seq 1 $USER_COUNT); do
    user_response=$(curl -s -X POST http://localhost:8080/api/auth/register \
      -H "Content-Type: application/json" \
      -d "{
        \"username\": \"flashuser$i\",
        \"email\": \"flashuser$i@shop.com\",
        \"password\": \"flash123\",
        \"firstName\": \"Flash\",
        \"lastName\": \"User$i\",
        \"phoneNumber\": \"010-$(printf "%04d" $i)-5678\",
        \"address\": \"서울시 강남구 플래시 $i번지\"
      }" 2>/dev/null)
    
    if [[ $user_response == *"successfully"* ]]; then
        # 로그인하여 토큰 획득
        login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
          -H "Content-Type: application/json" \
          -d "{
            \"usernameOrEmail\": \"flashuser$i\",
            \"password\": \"flash123\"
          }" 2>/dev/null)
        
        if [[ $login_response == *"token"* ]]; then
            TOKEN=$(echo $login_response | grep -o '"token":"[^"]*' | cut -d'"' -f4)
            TOKENS[$i]=$TOKEN
            echo -e "${GREEN}✅ 사용자 $i: 등록 및 로그인 완료${NC}"
        fi
    fi
done

echo -e "${GREEN}총 ${#TOKENS[@]}명의 플래시 세일 참여자 준비 완료${NC}"
echo ""

# 3단계: 플래시 세일 시작 알림
echo -e "${YELLOW}📢 3단계: 플래시 세일 시작!${NC}"
echo -e "${PURPLE}🚨 $CONCURRENT_USERS명이 동시에 구매 시도를 시작합니다...${NC}"

# 결과 저장용 배열
declare -A results
success_count=0
failed_count=0
error_count=0

# 임시 결과 파일
temp_file=$(mktemp)

# 4단계: 동시 재고 예약 테스트
echo -e "${YELLOW}⚡ 4단계: 동시 구매 시도 ($CONCURRENT_USERS개 요청)${NC}"

start_time=$(date +%s)

for i in $(seq 1 $CONCURRENT_USERS); do
    user_id=$((i % USER_COUNT + 1))
    order_id="FLASH_SALE_ORDER_$(date +%s%N)_$i"
    
    (
        response=$(curl -s -X POST http://localhost:8080/api/products/reserve-stock \
          -H "Content-Type: application/json" \
          -d "{
            \"productId\": $PRODUCT_ID,
            \"quantity\": 1,
            \"userId\": $user_id,
            \"orderId\": \"$order_id\"
          }" 2>/dev/null)
        
        if [[ $response == *"success"* ]] && [[ $response == *"true"* ]]; then
            echo "SUCCESS:$i:$order_id" >> $temp_file
        elif [[ $response == *"success"* ]] && [[ $response == *"false"* ]]; then
            echo "FAILED:$i:$order_id" >> $temp_file
        else
            echo "ERROR:$i:$order_id" >> $temp_file
        fi
    ) &
done

# 모든 백그라운드 프로세스 완료 대기
wait

end_time=$(date +%s)
duration=$((end_time - start_time))

echo ""
echo -e "${YELLOW}📊 5단계: 결과 분석${NC}"

# 결과 집계
while IFS=: read -r status request_id order_id; do
    case $status in
        SUCCESS)
            ((success_count++))
            ;;
        FAILED)
            ((failed_count++))
            ;;
        ERROR)
            ((error_count++))
            ;;
    esac
done < $temp_file

# 결과 출력
echo -e "${GREEN}🎯 플래시 세일 결과:${NC}"
echo -e "${GREEN}   ✅ 성공한 구매: $success_count개${NC}"
echo -e "${RED}   ❌ 실패한 구매: $failed_count개 (재고 부족)${NC}"
echo -e "${YELLOW}   ⚠️  오류 발생: $error_count개${NC}"
echo -e "${BLUE}   ⏱️  총 소요 시간: ${duration}초${NC}"
echo ""

# 6단계: 재고 확인
echo -e "${YELLOW}📦 6단계: 최종 재고 확인${NC}"

final_product=$(curl -s http://localhost:8080/api/products/$PRODUCT_ID)
if [[ $final_product == *"availableStock"* ]]; then
    available_stock=$(echo $final_product | grep -o '"availableStock":[0-9]*' | cut -d':' -f2)
    reserved_stock=$(echo $final_product | grep -o '"reservedStock":[0-9]*' | cut -d':' -f2)
    total_stock=$(echo $final_product | grep -o '"stock":[0-9]*' | cut -d':' -f2)
    
    echo -e "${BLUE}📊 재고 현황:${NC}"
    echo -e "${BLUE}   - 전체 재고: $total_stock개${NC}"
    echo -e "${BLUE}   - 예약된 재고: $reserved_stock개${NC}"
    echo -e "${BLUE}   - 사용 가능한 재고: $available_stock개${NC}"
    
    # 재고 일관성 검증
    expected_reserved=$success_count
    if [ "$reserved_stock" -eq "$expected_reserved" ]; then
        echo -e "${GREEN}✅ 재고 일관성: 정상 (예약 성공 $success_count개 = 예약된 재고 $reserved_stock개)${NC}"
    else
        echo -e "${RED}⚠️  재고 일관성: 불일치 (예약 성공 $success_count개 ≠ 예약된 재고 $reserved_stock개)${NC}"
    fi
fi

echo ""

# 7단계: 성능 분석
echo -e "${YELLOW}⚡ 7단계: 성능 분석${NC}"

if [ $duration -gt 0 ]; then
    tps=$((CONCURRENT_USERS / duration))
    echo -e "${PURPLE}📈 처리 성능:${NC}"
    echo -e "${PURPLE}   - 동시 요청: $CONCURRENT_USERS개${NC}"
    echo -e "${PURPLE}   - 총 처리 시간: ${duration}초${NC}"
    echo -e "${PURPLE}   - 초당 처리량 (TPS): 약 ${tps} requests/sec${NC}"
    
    success_rate=$((success_count * 100 / CONCURRENT_USERS))
    echo -e "${PURPLE}   - 성공률: ${success_rate}%${NC}"
fi

echo ""

# 8단계: 시스템 상태 확인
echo -e "${YELLOW}🔍 8단계: 시스템 상태 확인${NC}"

# Gateway 상태
gateway_health=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*' | cut -d'"' -f4)
echo -e "${BLUE}   - Gateway 상태: $gateway_health${NC}"

# 각 서비스 상태 확인
services=("users:8082" "products:8081" "orders:8083" "payments:8084" "notifications:8085")

for service in "${services[@]}"; do
    name=$(echo $service | cut -d':' -f1)
    port=$(echo $service | cut -d':' -f2)
    
    status=$(curl -s -w "%{http_code}" http://localhost:$port/actuator/health -o /dev/null)
    if [ "$status" -eq 200 ]; then
        echo -e "${GREEN}   ✅ $name Service: 정상${NC}"
    else
        echo -e "${RED}   ❌ $name Service: 상태 확인 불가${NC}"
    fi
done

# 임시 파일 정리
rm -f $temp_file

echo ""
echo -e "${GREEN}🎉 플래시 세일 시나리오 테스트 완료!${NC}"
echo ""
echo -e "${PURPLE}📋 테스트 요약:${NC}"
echo -e "${PURPLE}   - 플래시 세일 상품: ⚡ 한정판 울트라북 ⚡${NC}"
echo -e "${PURPLE}   - 한정 수량: $FLASH_SALE_STOCK개${NC}"
echo -e "${PURPLE}   - 동시 구매 시도: $CONCURRENT_USERS명${NC}"
echo -e "${PURPLE}   - 구매 성공: $success_count명${NC}"
echo -e "${PURPLE}   - 시스템 처리 시간: ${duration}초${NC}"
echo ""
echo -e "${BLUE}🔍 추가 확인사항:${NC}"
echo -e "${BLUE}   1. Eureka Dashboard: http://localhost:8761${NC}"
echo -e "${BLUE}   2. Kafka UI: http://localhost:8090${NC}"
echo -e "${BLUE}   3. 상품 재고 확인: curl http://localhost:8080/api/products/$PRODUCT_ID${NC}"
echo ""
echo -e "${YELLOW}💡 실제 운영환경에서는 더 많은 동시 사용자와 더 적은 재고로 테스트해보세요!${NC}"