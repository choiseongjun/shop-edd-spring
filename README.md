# 🔥 플래시 세일 MSA 시스템

대용량 트래픽 처리가 가능한 플래시 세일 전용 마이크로서비스 아키텍처입니다.

## 🎯 핵심 시나리오
- **목표**: 동시접속자 10,000명, 초당 5,000 요청 처리
- **상황**: 한정 수량(1000개) 상품이 50% 할인으로 판매되는 1시간 플래시 세일

## 🏗️ 시스템 아키텍처

### 서비스 구성
| 서비스 | 포트 | 역할 |
|--------|------|------|
| **Eureka Server** | 8761 | 서비스 디스커버리 |
| **API Gateway** | 8080 | 라우팅, Rate Limiting, Circuit Breaker |
| **User Service** | 8082 | 사용자 인증, JWT 보안 |
| **Product Service** | 8081 | 상품 관리, Redis 캐싱, 재고 분산락 |
| **Order Service** | 8083 | 주문 처리, Redisson 분산락 |
| **Payment Service** | 8084 | 결제 처리, Circuit Breaker |
| **Notification Service** | 8085 | 실시간 알림, Kafka, WebSocket |

### 인프라 구성
- **PostgreSQL**: 각 서비스별 전용 데이터베이스
- **Redis**: 캐싱 + 분산락 처리
- **Kafka**: 이벤트 드리븐 아키텍처
- **Docker Compose**: 인프라 통합 관리

## 🚀 시작하기

### 1. 사전 요구사항
- Java 11+
- Docker & Docker Compose
- Gradle 7+

### 2. 인프라 시작
```bash
# PostgreSQL, Redis, Kafka 시작
docker-compose up -d

# 상태 확인
docker-compose ps
```

### 3. 서비스 시작 (순서 중요!)
```bash
# 1. 서비스 디스커버리
cd eureka-server && ./gradlew bootRun &

# 2. API Gateway
cd gateway && ./gradlew bootRun &

# 3. 각 마이크로서비스
cd user-service && ./gradlew bootRun &
cd product-service && ./gradlew bootRun &
cd order-service && ./gradlew bootRun &
cd payment-service && ./gradlew bootRun &
cd notification-service && ./gradlew bootRun &
```

### 4. 빠른 테스트
```bash
# 실행 권한 부여
chmod +x quick-test.sh
chmod +x flash-sale-scenario.sh

# 기본 기능 테스트
./quick-test.sh

# 플래시 세일 시나리오 테스트
./flash-sale-scenario.sh
```

## 🔧 플래시 세일 특화 기능

### 1. 동시성 제어
- **Redisson 분산락**: 재고 관리의 동시성 보장
- **PostgreSQL 비관적 락**: 데이터베이스 레벨 동시성 제어
- **재고 예약 시스템**: 주문 → 결제 → 확정 3단계 처리

### 2. 부하 제어
- **API Gateway Rate Limiting**:
  - 상품 조회: 100 req/s
  - 주문 처리: 200 req/s
  - 결제 처리: 100 req/s
- **서킷 브레이커**: 장애 격리 및 Fallback 처리

### 3. 성능 최적화
- **Redis 캐싱**: 상품 정보 5분 TTL
- **Connection Pool**: 각 서비스별 최적화된 DB 연결
- **비동기 처리**: Kafka 기반 이벤트 처리

### 4. 실시간 기능
- **WebSocket**: 실시간 재고 현황 알림
- **Kafka**: 주문/결제/알림 이벤트 스트리밍

## 📊 모니터링

### 대시보드
- **Eureka Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8090

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/circuitbreakers
```

## 📋 API 테스트

상세한 API 테스트 방법은 다음 파일들을 참고하세요:
- [`api-test-commands.md`](api-test-commands.md) - 전체 API 테스트 가이드
- [`quick-test.sh`](quick-test.sh) - 빠른 기본 기능 테스트
- [`flash-sale-scenario.sh`](flash-sale-scenario.sh) - 플래시 세일 시나리오 테스트

## 🔥 플래시 세일 테스트 시나리오

### 시나리오 1: 정상적인 플래시 세일
1. 1000개 한정 상품 등록
2. 50명이 동시에 구매 시도
3. 재고 소진까지의 처리 성능 측정

### 시나리오 2: 대량 트래픽 처리
1. 10,000개 상품 등록
2. 1000명이 동시에 구매 시도
3. Rate Limiting 및 Circuit Breaker 동작 확인

### 시나리오 3: 장애 복구
1. 플래시 세일 진행 중 결제 서비스 다운
2. Circuit Breaker 동작 확인
3. 서비스 복구 후 대기 중인 주문 처리

## 🛠️ 기술 스택

### Backend
- **Spring Boot 3.x**: 마이크로서비스 프레임워크
- **Spring Cloud Gateway**: API Gateway
- **Spring Cloud Netflix Eureka**: 서비스 디스커버리
- **Spring Security + JWT**: 인증/인가
- **Spring Data JPA**: 데이터 접근 계층

### Database & Cache
- **PostgreSQL**: 메인 데이터베이스
- **Redis**: 캐싱 + 분산락
- **Redisson**: Redis 기반 분산 시스템

### Messaging
- **Apache Kafka**: 이벤트 스트리밍
- **WebSocket**: 실시간 통신

### Resilience
- **Resilience4j**: Circuit Breaker, Retry, Rate Limiter
- **Spring Cloud OpenFeign**: 서비스 간 통신

## 📈 성능 목표 및 달성 결과

### 목표 스펙
- 동시 접속자: 10,000명
- 초당 처리량: 5,000 TPS
- 응답 시간: 평균 500ms 이하
- 가용성: 99.9% 이상

### 핵심 성능 최적화
1. **재고 관리**: 분산락으로 정확성 보장
2. **부하 분산**: Gateway Rate Limiting으로 과부하 방지
3. **장애 격리**: Circuit Breaker로 연쇄 장애 차단
4. **캐싱 전략**: Redis로 DB 부하 분산

## 🚨 주의사항

### 운영 환경 배포 시
1. **데이터베이스**: Connection Pool 크기 조정 필요
2. **Redis**: 메모리 사용량 모니터링 필수
3. **Kafka**: 파티션 수 조정으로 처리 성능 향상
4. **JVM**: Heap 사이즈 및 GC 튜닝 권장

### 보안
- JWT Secret Key 변경
- PostgreSQL 사용자 권한 최소화
- Rate Limiting 임계값 조정
- HTTPS 적용 권장

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**💡 이 프로젝트는 대용량 트래픽 처리를 위한 MSA 패턴과 플래시 세일 특화 기능을 학습할 수 있도록 설계되었습니다.**