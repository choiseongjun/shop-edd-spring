import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');
export let stockReservationRate = new Rate('stock_reservations');
export let orderCreationRate = new Rate('order_creations');
export let paymentProcessingRate = new Rate('payment_processing');

// 테스트 설정
export let options = {
  scenarios: {
    // 시나리오 1: 플래시 세일 준비 단계
    preparation: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 1,
      startTime: '0s',
      maxDuration: '1m',
      tags: { scenario: 'preparation' },
    },
    
    // 시나리오 2: 플래시 세일 시작 전 대기 (사용자들이 대기)
    warmup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 200 },  // 2분 동안 200명으로 증가
        { duration: '3m', target: 500 },  // 3분 동안 500명으로 증가
        { duration: '2m', target: 1000 }, // 2분 동안 1000명으로 증가
      ],
      startTime: '1m',
      tags: { scenario: 'warmup' },
    },
    
    // 시나리오 3: 플래시 세일 폭발적 접근 (동시 구매 시도)
    flash_sale_burst: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      stages: [
        { duration: '10s', target: 500 },   // 10초 동안 초당 500 요청
        { duration: '30s', target: 2000 },  // 30초 동안 초당 2000 요청
        { duration: '1m', target: 5000 },   // 1분 동안 초당 5000 요청 (최대 부하)
        { duration: '30s', target: 3000 },  // 30초 동안 초당 3000 요청
        { duration: '1m', target: 1000 },   // 1분 동안 초당 1000 요청으로 감소
        { duration: '30s', target: 100 },   // 30초 동안 초당 100 요청으로 감소
      ],
      preAllocatedVUs: 1000,
      maxVUs: 10000,
      startTime: '8m',
      tags: { scenario: 'flash_sale' },
    },
    
    // 시나리오 4: 일반 트래픽 (플래시 세일과 동시에 일반 사용자들)
    normal_traffic: {
      executor: 'constant-vus',
      vus: 100,
      duration: '15m',
      startTime: '8m',
      tags: { scenario: 'normal' },
    },
  },
  
  thresholds: {
    http_req_duration: ['p(95)<5000'], // 95%의 요청이 5초 이내
    http_req_failed: ['rate<0.2'],     // 실패율 20% 이하
    errors: ['rate<0.25'],             // 에러율 25% 이하
    success: ['rate>0.75'],            // 성공율 75% 이상
    stock_reservations: ['rate>0.1'],  // 재고 예약 성공율 10% 이상
    order_creations: ['rate>0.05'],    // 주문 생성 성공율 5% 이상
    payment_processing: ['rate>0.03'], // 결제 처리 성공율 3% 이상
  },
};

// 테스트 데이터
const BASE_URL = 'http://localhost:8080';
let authToken = '';
let userId = Math.floor(Math.random() * 100000) + 1;
let flashSaleProductId = 2;

// 시나리오별 함수들
export function setup() {
  console.log('플래시 세일 테스트 환경 설정 시작...');
  
  // 관리자 로그인
  let adminLoginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    usernameOrEmail: 'admin',
    password: 'admin123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  let adminToken = '';
  if (adminLoginRes.status === 200) {
    adminToken = adminLoginRes.json('token');
  }
  
  // 플래시 세일 상품 생성/업데이트
  if (adminToken) {
    let createProductRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
      name: '한정판 스마트폰',
      description: '1000개 한정! 70% 할인',
      price: 300000,
      originalPrice: 1000000,
      stock: 1000,
      categoryId: 1,
      imageUrl: 'https://example.com/phone.jpg',
      active: true,
      flashSale: true,
      flashSaleStartTime: new Date(Date.now() + 8 * 60 * 1000).toISOString(), // 8분 후 시작
      flashSaleEndTime: new Date(Date.now() + 12 * 60 * 1000).toISOString(),   // 12분 후 종료
      flashSaleStock: 1000,
      discountRate: 70
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
    });
    
    if (createProductRes.status === 201) {
      flashSaleProductId = createProductRes.json('id');
      console.log(`플래시 세일 상품 생성됨: ID ${flashSaleProductId}`);
    }
  }
  
  return { adminToken: adminToken, flashSaleProductId: flashSaleProductId };
}

export default function(data) {
  const scenario = __ENV.SCENARIO || 'flash_sale';
  
  switch(scenario) {
    case 'preparation':
      preparationScenario();
      break;
    case 'warmup':
      warmupScenario(data);
      break;
    case 'flash_sale':
      flashSaleScenario(data);
      break;
    case 'normal':
      normalTrafficScenario(data);
      break;
    default:
      flashSaleScenario(data);
  }
}

// 준비 시나리오: 사용자 등록
function preparationScenario() {
  let registerRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
    username: `user${userId}`,
    email: `user${userId}@example.com`,
    password: 'password123',
    firstName: 'Test',
    lastName: 'User',
    phoneNumber: `010-${String(userId).padStart(4, '0')}-${String(userId).padStart(4, '0')}`,
    address: '서울시 강남구'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(registerRes, {
    'user registration successful': (r) => r.status === 201 || r.status === 409,
  }) || errorRate.add(1);
  
  sleep(0.1);
}

// 워밍업 시나리오: 상품 조회, 로그인
function warmupScenario(data) {
  // 상품 목록 조회
  let productsRes = http.get(`${BASE_URL}/api/products`);
  check(productsRes, {
    'products list loaded': (r) => r.status === 200,
  }) || errorRate.add(1);
  
  // 플래시 세일 상품 상세 조회
  let productRes = http.get(`${BASE_URL}/api/products/${data.flashSaleProductId || 2}`);
  check(productRes, {
    'flash sale product loaded': (r) => r.status === 200,
  }) || errorRate.add(1);
  
  // 사용자 로그인
  let loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    usernameOrEmail: `user${userId}`,
    password: 'password123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (loginRes.status === 200) {
    authToken = loginRes.json('token');
  }
  
  check(loginRes, {
    'user login successful': (r) => r.status === 200,
  }) || errorRate.add(1);
  
  sleep(Math.random() * 3 + 1); // 1-4초 랜덤 대기
}

// 플래시 세일 시나리오: 동시 구매 시도
function flashSaleScenario(data) {
  if (!authToken) {
    // 로그인이 안 된 경우 로그인 시도
    let loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      usernameOrEmail: `user${userId}`,
      password: 'password123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (loginRes.status === 200) {
      authToken = loginRes.json('token');
    }
  }
  
  if (authToken) {
    // 1. 플래시 세일 상품 조회
    let productRes = http.get(`${BASE_URL}/api/products/${data.flashSaleProductId || 2}`);
    let productSuccess = check(productRes, {
      'flash sale product loaded': (r) => r.status === 200,
    });
    
    // 2. 재고 예약 시도 (분산 락 테스트)
    let orderId = `FLASH_ORDER_${userId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    let reserveRes = http.post(`${BASE_URL}/api/products/reserve-stock`, JSON.stringify({
      productId: data.flashSaleProductId || 2,
      quantity: 1,
      userId: userId,
      orderId: orderId
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
    });
    
    let reserveSuccess = check(reserveRes, {
      'stock reservation attempted': (r) => r.status === 200 || r.status === 409 || r.status === 400,
      'stock reservation successful': (r) => r.status === 200,
    });
    
    if (reserveSuccess && reserveRes.status === 200) {
      stockReservationRate.add(1);
      
      // 3. 주문 생성 시도
      let orderRes = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
        orderItems: [{
          productId: data.flashSaleProductId || 2,
          quantity: 1,
          unitPrice: 300000,
          flashSaleItem: true
        }],
        shippingAddress: '서울시 강남구 테헤란로 123',
        paymentMethod: 'CREDIT_CARD',
        flashSaleOrder: true
      }), {
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      });
      
      let orderSuccess = check(orderRes, {
        'flash sale order created': (r) => r.status === 201,
      });
      
      if (orderSuccess) {
        orderCreationRate.add(1);
        let actualOrderId = orderRes.json('order.orderId');
        
        // 4. 결제 처리 시도
        let paymentRes = http.post(`${BASE_URL}/api/payments/process`, JSON.stringify({
          orderId: actualOrderId,
          amount: 300000,
          paymentMethod: 'CREDIT_CARD',
          flashSalePayment: true
        }), {
          headers: { 
            'Content-Type': 'application/json',
            'User-Id': userId.toString()
          },
        });
        
        let paymentSuccess = check(paymentRes, {
          'flash sale payment processed': (r) => r.status === 200,
        });
        
        if (paymentSuccess) {
          paymentProcessingRate.add(1);
        }
      }
    }
    
    // 전체 성공 여부 확인
    if (productSuccess) {
      successRate.add(1);
    } else {
      errorRate.add(1);
    }
  } else {
    errorRate.add(1);
  }
  
  sleep(0.05); // 매우 짧은 대기로 빠른 반복
}

// 일반 트래픽 시나리오
function normalTrafficScenario(data) {
  // 일반적인 쇼핑몰 사용 패턴
  let actions = [
    () => http.get(`${BASE_URL}/api/products`),
    () => http.get(`${BASE_URL}/api/products/${Math.floor(Math.random() * 10) + 1}`),
    () => http.get(`${BASE_URL}/api/products/search?keyword=phone`),
    () => http.get(`${BASE_URL}/api/products/category/1`),
  ];
  
  let randomAction = actions[Math.floor(Math.random() * actions.length)];
  let res = randomAction();
  
  let success = check(res, {
    'normal traffic request successful': (r) => r.status === 200,
  });
  
  if (success) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(Math.random() * 3 + 1); // 1-4초 랜덤 대기
}

export function teardown(data) {
  console.log('플래시 세일 테스트 정리 중...');
  // 테스트 후 정리 작업이 필요한 경우
}

