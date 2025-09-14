# K6 Load Testing for Flash Sale MSA

플래시 세일 마이크로서비스 아키텍처를 위한 K6 부하 테스트 시나리오입니다.

## 사전 요구사항

### K6 설치

**Windows:**
```bash
winget install k6
# 또는 Chocolatey 사용
choco install k6
```

**macOS:**
```bash
brew install k6
```

**Linux (Ubuntu/Debian):**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## 테스트 시나리오

### 1. 종합 플로우 테스트 (Comprehensive Flow Test)
- **목적**: 회원가입부터 결제까지 전체 플로우 테스트
- **사용자**: 최대 100명 동시 접속
- **지속시간**: 10분
- **특징**: 실제 사용자 행동 패턴 시뮬레이션, 일반 쇼핑과 플래시 세일 동시 테스트

### 2. 플래시 세일 스트레스 테스트 (Flash Sale Stress Test)
- **목적**: 플래시 세일 대용량 트래픽 처리 능력 테스트
- **사용자**: 최대 10,000명 동시 접속
- **지속시간**: 15분
- **특징**: 
  - 초당 5,000 요청까지 테스트
  - 분산 락과 재고 관리 시스템 검증
  - 동시성 문제와 경합 상태 테스트

### 3. 시스템 한계 테스트 (System Limits Test)
- **목적**: 시스템의 최대 처리 능력과 한계점 확인
- **사용자**: 최대 3,000명 동시 접속
- **지속시간**: 65분
- **특징**: 점진적 부하 증가, 스파이크 테스트, 메모리 누수 테스트

### 4. API별 개별 테스트 (API Specific Tests)
- **목적**: 각 API의 독립적 성능과 안정성 검증
- **사용자**: API별로 20-100명 동시 접속
- **지속시간**: 25분
- **특징**: 인증, 상품, 주문, 결제, 알림 API 개별 테스트

### 5. 간단한 부하 테스트 (Simple Load Test)
- **목적**: 기본적인 API 동작 및 성능 확인
- **사용자**: 최대 200명 동시 접속
- **지속시간**: 5분
- **API**: 상품 목록, 상품 상세, 사용자 프로필

### 6. 스트레스 테스트 (Stress Test)
- **목적**: 시스템 한계점 확인
- **사용자**: 최대 1,500명 동시 접속
- **지속시간**: 9분
- **특징**: 동시 API 호출, 재고 예약 경쟁 상황

## 실행 방법

### 1. 모든 테스트 실행 (권장)

**Windows:**
```bash
cd C:\project\distributed-shop\k6-load-test
run-comprehensive-tests.bat
```

**Linux/macOS:**
```bash
cd /path/to/distributed-shop/k6-load-test
chmod +x run-comprehensive-tests.sh
./run-comprehensive-tests.sh
```

### 2. 개별 테스트 실행

**Windows:**
```bash
run-individual-tests.bat [테스트명]
```

**Linux/macOS:**
```bash
chmod +x run-individual-tests.sh
./run-individual-tests.sh [테스트명]
```

**사용 가능한 테스트명:**
- `comprehensive`: 종합 플로우 테스트
- `flash-sale`: 플래시 세일 스트레스 테스트
- `system-limits`: 시스템 한계 테스트
- `api-specific`: API별 개별 테스트
- `simple`: 간단한 부하 테스트
- `stress`: 스트레스 테스트

### 3. 직접 실행

```bash
# 종합 플로우 테스트
k6 run comprehensive-flow-test.js

# 플래시 세일 스트레스 테스트
k6 run flash-sale-stress-test.js

# 시스템 한계 테스트
k6 run system-limits-test.js

# API별 개별 테스트
k6 run api-specific-tests.js

# 간단한 부하 테스트
k6 run scenarios/simple-load-test.js

# 스트레스 테스트  
k6 run scenarios/stress-test.js
```

### 3. 옵션과 함께 실행

```bash
# HTML 리포트 생성
k6 run --out html=report.html flash-sale-test.js

# JSON 결과 저장
k6 run --out json=results.json scenarios/stress-test.js

# InfluxDB로 실시간 모니터링
k6 run --out influxdb=http://localhost:8086/k6 flash-sale-test.js
```

## 메트릭 및 임계값

### 기본 메트릭
- **http_req_duration**: HTTP 요청 응답 시간
- **http_req_failed**: HTTP 요청 실패율
- **errors**: 사용자 정의 에러율

### 임계값 설정

| 테스트 유형 | 응답시간 (95%) | 실패율 | 에러율 |
|------------|---------------|--------|--------|
| 간단한 부하 테스트 | < 1초 | < 10% | < 10% |
| 스트레스 테스트 | < 5초 | < 20% | < 30% |
| 플래시 세일 | < 2초 | < 5% | < 10% |

## 시나리오 상세

### 플래시 세일 시나리오 단계

1. **Setup Phase (0-30초)**
   - 관리자 로그인
   - 플래시 세일 상품 생성
   - 사용자 계정 생성

2. **Warmup Phase (30초-3분30초)**
   - 점진적 사용자 증가 (0 → 500명)
   - 상품 목록/상세 조회
   - 사용자 로그인

3. **Flash Sale Burst (3분30초-6분)**
   - 폭발적 트래픽 증가 (초당 100 → 5,000 요청)
   - 동시 재고 예약 시도
   - 주문 및 결제 처리

4. **Normal Traffic (4분-14분)**
   - 일반적인 쇼핑몰 트래픽 (50명 동시)
   - 상품 검색, 조회 등

## 모니터링

### 실시간 모니터링 설정

K6와 함께 다음 도구들을 사용하여 실시간 모니터링:

1. **InfluxDB + Grafana**
```bash
# Docker로 InfluxDB 실행
docker run -d -p 8086:8086 influxdb:1.8

# K6 결과를 InfluxDB로 전송
k6 run --out influxdb=http://localhost:8086/k6 flash-sale-test.js
```

2. **K6 Web Dashboard**
```bash
# K6 Web Dashboard 실행
k6 run --out web-dashboard flash-sale-test.js
```

### 주요 모니터링 포인트

- **응답 시간 분포**: P50, P95, P99
- **처리량**: RPS (Requests Per Second)
- **에러율**: HTTP 4xx, 5xx 응답
- **동시 사용자 수**: Active VUs
- **리소스 사용률**: CPU, 메모리, 네트워크

## 결과 분석

### 성공 기준

1. **기능적 요구사항**
   - 모든 API가 정상 동작
   - 사용자 인증/인가 정상
   - 플래시 세일 재고 관리 정확성

2. **성능 요구사항**
   - 동시 10,000명 처리 가능
   - 초당 5,000 요청 처리
   - 95% 응답시간 2초 이내
   - 실패율 5% 이하

3. **안정성 요구사항**
   - 분산 락 정상 동작
   - 서킷 브레이커 동작
   - 데이터 일관성 유지

### 문제 해결

일반적인 성능 문제와 해결 방안:

1. **높은 응답시간**
   - DB 연결 풀 크기 조정
   - Redis 캐시 활용
   - DB 인덱스 최적화

2. **높은 실패율**
   - 서킷 브레이커 임계값 조정
   - 타임아웃 설정 검토
   - 재시도 정책 구현

3. **재고 동시성 문제**
   - 분산 락 타임아웃 조정
   - 낙관적 락 적용 검토
   - 재고 예약 로직 최적화

## 참고 자료

- [K6 공식 문서](https://k6.io/docs/)
- [K6 시나리오 가이드](https://k6.io/docs/using-k6/scenarios/)
- [K6 메트릭 가이드](https://k6.io/docs/using-k6/metrics/)