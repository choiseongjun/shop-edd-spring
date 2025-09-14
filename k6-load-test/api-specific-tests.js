import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');
export let authErrorRate = new Rate('auth_errors');
export let productErrorRate = new Rate('product_errors');
export let orderErrorRate = new Rate('order_errors');
export let paymentErrorRate = new Rate('payment_errors');

// 테스트 설정
export let options = {
  scenarios: {
    // 시나리오 1: 인증 API 테스트
    auth_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },   // 1분 동안 50명으로 증가
        { duration: '2m', target: 100 },  // 2분 동안 100명으로 증가
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '0s',
      tags: { scenario: 'auth' },
    },
    
    // 시나리오 2: 상품 API 테스트
    product_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },  // 1분 동안 100명으로 증가
        { duration: '2m', target: 200 },  // 2분 동안 200명으로 증가
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '5m',
      tags: { scenario: 'product' },
    },
    
    // 시나리오 3: 주문 API 테스트
    order_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },   // 1분 동안 30명으로 증가
        { duration: '2m', target: 50 },   // 2분 동안 50명으로 증가
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '10m',
      tags: { scenario: 'order' },
    },
    
    // 시나리오 4: 결제 API 테스트
    payment_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },   // 1분 동안 20명으로 증가
        { duration: '2m', target: 30 },   // 2분 동안 30명으로 증가
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '15m',
      tags: { scenario: 'payment' },
    },
    
    // 시나리오 5: 알림 API 테스트
    notification_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },   // 1분 동안 50명으로 증가
        { duration: '2m', target: 100 },  // 2분 동안 100명으로 증가
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '20m',
      tags: { scenario: 'notification' },
    },
  },
  
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 이내
    http_req_failed: ['rate<0.1'],     // 실패율 10% 이하
    errors: ['rate<0.1'],              // 에러율 10% 이하
    success: ['rate>0.9'],             // 성공율 90% 이상
    auth_errors: ['rate<0.05'],        // 인증 에러율 5% 이하
    product_errors: ['rate<0.05'],     // 상품 에러율 5% 이하
    order_errors: ['rate<0.1'],        // 주문 에러율 10% 이하
    payment_errors: ['rate<0.15'],     // 결제 에러율 15% 이하
  },
};

// 테스트 데이터
const BASE_URL = 'http://localhost:8080';
let authToken = '';
let userId = Math.floor(Math.random() * 100000) + 1;
let productId = 1;
let orderId = '';

// 시나리오별 함수들
export function setup() {
  console.log('API별 개별 테스트 환경 설정 시작...');
  
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
  
  // 테스트용 상품 생성
  if (adminToken) {
    let createProductRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
      name: 'API 테스트 상품',
      description: 'API별 개별 테스트용 상품입니다.',
      price: 50000,
      originalPrice: 75000,
      stock: 1000,
      categoryId: 1,
      imageUrl: 'https://example.com/api-test.jpg',
      active: true,
      flashSale: false
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
    });
    
    if (createProductRes.status === 201) {
      productId = createProductRes.json('id');
      console.log(`API 테스트 상품 생성됨: ID ${productId}`);
    }
  }
  
  return { adminToken: adminToken, productId: productId };
}

export default function(data) {
  const scenario = __ENV.SCENARIO || 'auth';
  
  switch(scenario) {
    case 'auth':
      authTestScenario();
      break;
    case 'product':
      productTestScenario(data);
      break;
    case 'order':
      orderTestScenario(data);
      break;
    case 'payment':
      paymentTestScenario(data);
      break;
    case 'notification':
      notificationTestScenario(data);
      break;
    default:
      authTestScenario();
  }
}

// 인증 API 테스트
function authTestScenario() {
  let actions = [
    // 사용자 등록
    () => http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username: `user${userId}`,
      email: `user${userId}@example.com`,
      password: 'password123',
      firstName: 'Test',
      lastName: 'User',
      phoneNumber: `010-${String(userId).padStart(4, '0')}-${String(userId).padStart(4, '0')}`,
      address: '서울시 강남구'
    }), {
      headers: { 'Content-Type': 'application/json' },
    }),
    
    // 로그인
    () => http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      usernameOrEmail: `user${userId}`,
      password: 'password123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    }),
    
    // 토큰 검증
    () => http.post(`${BASE_URL}/api/auth/validate`, null, {
      headers: { 'Authorization': `Bearer ${authToken}` },
    }),
  ];
  
  let randomAction = actions[Math.floor(Math.random() * actions.length)];
  let res = randomAction();
  
  let success = check(res, {
    'auth request successful': (r) => r.status === 200 || r.status === 201 || r.status === 409,
    'auth response time < 2s': (r) => r.timings.duration < 2000,
  });
  
  if (success) {
    successRate.add(1);
    if (res.status === 200 && res.url.includes('/login')) {
      authToken = res.json('token');
    }
  } else {
    errorRate.add(1);
    authErrorRate.add(1);
  }
  
  sleep(0.5);
}

// 상품 API 테스트
function productTestScenario(data) {
  let actions = [
    // 상품 목록 조회
    () => http.get(`${BASE_URL}/api/products`),
    
    // 상품 상세 조회
    () => http.get(`${BASE_URL}/api/products/${data.productId || 1}`),
    
    // 상품 검색
    () => http.get(`${BASE_URL}/api/products/search?keyword=테스트`),
    
    // 카테고리별 상품 조회
    () => http.get(`${BASE_URL}/api/products/category/1`),
    
    // 플래시 세일 상품 조회
    () => http.get(`${BASE_URL}/api/products/flash-sale`),
  ];
  
  let randomAction = actions[Math.floor(Math.random() * actions.length)];
  let res = randomAction();
  
  let success = check(res, {
    'product request successful': (r) => r.status === 200,
    'product response time < 2s': (r) => r.timings.duration < 2000,
  });
  
  if (success) {
    successRate.add(1);
  } else {
    errorRate.add(1);
    productErrorRate.add(1);
  }
  
  sleep(0.3);
}

// 주문 API 테스트
function orderTestScenario(data) {
  // 로그인 확인
  if (!authToken) {
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
    let actions = [
      // 주문 생성
      () => http.post(`${BASE_URL}/api/orders`, JSON.stringify({
        orderItems: [{
          productId: data.productId || 1,
          quantity: Math.floor(Math.random() * 3) + 1,
          unitPrice: 50000,
          flashSaleItem: false
        }],
        shippingAddress: '서울시 강남구 테헤란로 123',
        paymentMethod: 'CREDIT_CARD',
        flashSaleOrder: false
      }), {
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
      
      // 사용자 주문 조회
      () => http.get(`${BASE_URL}/api/orders`, {
        headers: { 
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
      
      // 특정 주문 조회
      () => http.get(`${BASE_URL}/api/orders/${orderId}`, {
        headers: { 
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
    ];
    
    let randomAction = actions[Math.floor(Math.random() * actions.length)];
    let res = randomAction();
    
    let success = check(res, {
      'order request successful': (r) => r.status === 200 || r.status === 201 || r.status === 404,
      'order response time < 3s': (r) => r.timings.duration < 3000,
    });
    
    if (success) {
      successRate.add(1);
      if (res.status === 201 && res.url.includes('/orders') && !res.url.includes('/orders/')) {
        orderId = res.json('order.orderId');
      }
    } else {
      errorRate.add(1);
      orderErrorRate.add(1);
    }
  } else {
    errorRate.add(1);
    orderErrorRate.add(1);
  }
  
  sleep(1);
}

// 결제 API 테스트
function paymentTestScenario(data) {
  // 주문이 있는 경우에만 결제 테스트
  if (orderId) {
    let actions = [
      // 결제 처리
      () => http.post(`${BASE_URL}/api/payments/process`, JSON.stringify({
        orderId: orderId,
        amount: 50000,
        paymentMethod: 'CREDIT_CARD',
        flashSalePayment: false
      }), {
        headers: { 
          'Content-Type': 'application/json',
          'User-Id': userId.toString()
        },
      }),
      
      // 결제 상태 조회
      () => http.get(`${BASE_URL}/api/payments/${orderId}/status`, {
        headers: { 'User-Id': userId.toString() },
      }),
      
      // 사용자 결제 내역 조회
      () => http.get(`${BASE_URL}/api/payments/user`, {
        headers: { 'User-Id': userId.toString() },
      }),
    ];
    
    let randomAction = actions[Math.floor(Math.random() * actions.length)];
    let res = randomAction();
    
    let success = check(res, {
      'payment request successful': (r) => r.status === 200 || r.status === 201,
      'payment response time < 5s': (r) => r.timings.duration < 5000,
    });
    
    if (success) {
      successRate.add(1);
    } else {
      errorRate.add(1);
      paymentErrorRate.add(1);
    }
  } else {
    // 주문이 없는 경우 더미 결제 테스트
    let res = http.post(`${BASE_URL}/api/payments/process`, JSON.stringify({
      orderId: 'DUMMY_ORDER',
      amount: 1000,
      paymentMethod: 'CREDIT_CARD',
      flashSalePayment: false
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'User-Id': userId.toString()
      },
    });
    
    check(res, {
      'dummy payment handled': (r) => r.status === 400 || r.status === 404,
    });
  }
  
  sleep(1);
}

// 알림 API 테스트
function notificationTestScenario(data) {
  // 로그인 확인
  if (!authToken) {
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
    let actions = [
      // 사용자 알림 조회
      () => http.get(`${BASE_URL}/api/notifications/user`, {
        headers: { 
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
      
      // 읽지 않은 알림 수 조회
      () => http.get(`${BASE_URL}/api/notifications/unread-count`, {
        headers: { 
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
      
      // 알림 전송 (관리자 기능)
      () => http.post(`${BASE_URL}/api/notifications/send`, JSON.stringify({
        topic: '/topic/flash-sale',
        message: '플래시 세일이 시작되었습니다!',
        userId: userId
      }), {
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
          'User-Id': userId.toString()
        },
      }),
    ];
    
    let randomAction = actions[Math.floor(Math.random() * actions.length)];
    let res = randomAction();
    
    let success = check(res, {
      'notification request successful': (r) => r.status === 200 || r.status === 201,
      'notification response time < 2s': (r) => r.timings.duration < 2000,
    });
    
    if (success) {
      successRate.add(1);
    } else {
      errorRate.add(1);
    }
  } else {
    errorRate.add(1);
  }
  
  sleep(0.5);
}

export function teardown(data) {
  console.log('API별 개별 테스트 정리 중...');
  // 테스트 후 정리 작업이 필요한 경우
}
