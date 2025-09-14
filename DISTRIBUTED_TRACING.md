# 🔍 분산 추적(Distributed Tracing) 가이드

이 프로젝트에 Zipkin을 사용한 분산 추적이 구현되어 있습니다. 주문 생성 과정에서 서비스 간 호출을 추적할 수 있습니다.

## 🚀 시작하기

### 1. Zipkin 서버 시작
```bash
# Docker Compose로 Zipkin 포함 모든 인프라 시작
docker-compose up -d zipkin
```

### 2. 서비스 시작
```bash
# 각 서비스를 순서대로 시작
# 1. Eureka Server
# 2. Gateway
# 3. Product Service
# 4. Order Service
# 5. Payment Service
# 6. Notification Service
```

### 3. 분산 추적 테스트
```bash
# 테스트 스크립트 실행
chmod +x test-distributed-tracing.sh
./test-distributed-tracing.sh
```

## 📊 Zipkin UI 사용법

### 접속 정보
- **Zipkin UI**: http://localhost:9411

### 추적 확인 방법
1. **Find Traces** 버튼 클릭
2. **Service Name**에서 `order-service` 선택
3. **Search** 버튼으로 최근 추적 정보 조회

## 🏷️ 구현된 추적 스팬(Spans)

### 주문 생성 플로우
```
order.create (Root Span)
├── order.validation
├── order.items.processing
│   └── product-service.get-product
├── order.save
└── order.event.publish
```

### 스팬별 상세 정보

#### 1. `order.create` (Root Span)
- **설명**: 전체 주문 생성 프로세스
- **태그**:
  - `user.id`: 주문하는 사용자 ID
  - `order.items.count`: 주문 아이템 개수
  - `order.flash.sale`: 플래시 세일 여부
  - `order.id`: 생성된 주문 ID
  - `order.status`: 주문 상태

#### 2. `order.validation`
- **설명**: 주문 요청 검증 과정
- **태그**:
  - `user.id`: 검증 대상 사용자 ID

#### 3. `order.items.processing`
- **설명**: 주문 아이템 처리 및 총액 계산
- **태그**:
  - `items.count`: 처리된 아이템 수
  - `order.total.amount`: 계산된 총액

#### 4. `product-service.get-product`
- **설명**: 상품 서비스에서 상품 정보 조회
- **태그**:
  - `product.id`: 조회한 상품 ID
  - `service.name`: product-service
  - `http.url`: 호출된 URL
  - `http.status`: HTTP 응답 상태
  - `product.name`: 상품명
  - `product.price`: 상품 가격

#### 5. `order.save`
- **설명**: 데이터베이스에 주문 저장
- **태그**:
  - `order.id`: 저장된 주문 ID

#### 6. `order.event.publish`
- **설명**: 주문 생성 이벤트 Kafka 발행
- **태그**:
  - `order.id`: 주문 ID
  - `event.type`: OrderCreated

## 🔧 설정 정보

### 트레이싱 설정 (application.yml)
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 샘플링
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 의존성 (build.gradle)
```gradle
// Distributed Tracing
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
implementation 'io.micrometer:micrometer-observation'
```

## 🧪 테스트 시나리오

### 기본 주문 생성 테스트
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "User-Id: 1" \
  -H "X-Trace-Id: manual-trace-test" \
  -d '{
    "orderItems": [{"productId": 1, "quantity": 2}],
    "shippingAddress": "서울시 강남구",
    "paymentMethod": "CREDIT_CARD"
  }'
```

### 에러 상황 테스트
```bash
# 존재하지 않는 상품 주문 (에러 추적 확인)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "User-Id: 1" \
  -d '{
    "orderItems": [{"productId": 999, "quantity": 1}],
    "shippingAddress": "서울시 강남구",
    "paymentMethod": "CREDIT_CARD"
  }'
```

## 📈 모니터링 포인트

### 성능 메트릭
- **응답 시간**: 각 스팬의 duration 확인
- **병목 구간**: 가장 오래 걸리는 스팬 식별
- **에러율**: error 태그가 있는 스팬 비율

### 비즈니스 메트릭
- **주문 성공률**: 성공/실패 주문 비율
- **상품별 주문량**: product.id 태그 분석
- **사용자별 주문 패턴**: user.id 태그 분석

## 🛠️ 추가 개발 가이드

### 새로운 스팬 추가
```java
@Service
public class MyService {
    private final Tracer tracer;

    public void myMethod() {
        Span span = tracer.nextSpan()
            .name("my.custom.operation")
            .tag("custom.tag", "value")
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 비즈니스 로직
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 어노테이션 기반 추적
```java
@Observed(name = "user.login", contextualName = "user-authentication")
public UserDto loginUser(LoginRequest request) {
    // 자동으로 추적됨
}
```

## 🚨 주의사항

1. **샘플링 비율**: 운영 환경에서는 `sampling.probability`를 0.1 (10%) 정도로 설정
2. **성능 영향**: 추적 오버헤드를 고려하여 필요한 스팬만 생성
3. **민감 정보**: 태그에 개인정보나 민감한 데이터 포함 금지
4. **스토리지**: Zipkin 운영 환경에서는 Elasticsearch나 MySQL 백엔드 사용 권장

## 🔗 유용한 링크

- [Zipkin Documentation](https://zipkin.io/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)