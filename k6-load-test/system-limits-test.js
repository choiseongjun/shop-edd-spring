import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');
export let timeoutRate = new Rate('timeouts');
export let circuitBreakerRate = new Rate('circuit_breakers');

// 테스트 설정
export let options = {
  scenarios: {
    // 시나리오 1: 점진적 부하 증가 (시스템 한계 찾기)
    gradual_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },    // 2분 동안 50명으로 증가
        { duration: '2m', target: 100 },   // 2분 동안 100명으로 증가
        { duration: '2m', target: 200 },   // 2분 동안 200명으로 증가
        { duration: '2m', target: 500 },   // 2분 동안 500명으로 증가
        { duration: '2m', target: 1000 },  // 2분 동안 1000명으로 증가
        { duration: '2m', target: 1500 },  // 2분 동안 1500명으로 증가
        { duration: '2m', target: 2000 },  // 2분 동안 2000명으로 증가
        { duration: '2m', target: 3000 },  // 2분 동안 3000명으로 증가 (한계 테스트)
        { duration: '2m', target: 0 },     // 2분 동안 0명으로 감소
      ],
      tags: { scenario: 'gradual_load' },
    },
    
    // 시나리오 2: 스파이크 테스트 (갑작스러운 트래픽 증가)
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },   // 1분 동안 100명으로 증가
        { duration: '30s', target: 2000 }, // 30초 동안 2000명으로 급증
        { duration: '1m', target: 2000 },  // 1분 동안 2000명 유지
        { duration: '30s', target: 100 },  // 30초 동안 100명으로 급감
        { duration: '1m', target: 0 },     // 1분 동안 0명으로 감소
      ],
      startTime: '20m',
      tags: { scenario: 'spike_test' },
    },
    
    // 시나리오 3: 지속적 고부하 테스트
    sustained_load: {
      executor: 'constant-vus',
      vus: 1000,
      duration: '10m',
      startTime: '25m',
      tags: { scenario: 'sustained_load' },
    },
    
    // 시나리오 4: 메모리 누수 테스트 (장시간 실행)
    memory_leak_test: {
      executor: 'constant-vus',
      vus: 500,
      duration: '30m',
      startTime: '35m',
      tags: { scenario: 'memory_leak' },
    },
  },
  
  thresholds: {
    http_req_duration: ['p(95)<10000'], // 95%의 요청이 10초 이내
    http_req_failed: ['rate<0.5'],      // 실패율 50% 이하
    errors: ['rate<0.6'],               // 에러율 60% 이하
    success: ['rate>0.4'],              // 성공율 40% 이상
    timeouts: ['rate<0.3'],             // 타임아웃율 30% 이하
  },
};

// 테스트 데이터
const BASE_URL = 'http://localhost:8080';
let authToken = '';
let userId = Math.floor(Math.random() * 100000) + 1;
let productId = 1;

// 시나리오별 함수들
export function setup() {
  console.log('시스템 한계 테스트 환경 설정 시작...');
  
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
  
  // 테스트용 상품들 생성
  if (adminToken) {
    for (let i = 1; i <= 10; i++) {
      let createProductRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
        name: `테스트 상품 ${i}`,
        description: `시스템 한계 테스트용 상품 ${i}입니다.`,
        price: 10000 * i,
        originalPrice: 15000 * i,
        stock: 1000,
        categoryId: 1,
        imageUrl: `https://example.com/product${i}.jpg`,
        active: true,
        flashSale: false
      }), {
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${adminToken}`
        },
      });
      
      if (createProductRes.status === 201) {
        console.log(`테스트 상품 ${i} 생성됨`);
      }
    }
  }
  
  return { adminToken: adminToken };
}

export default function(data) {
  const scenario = __ENV.SCENARIO || 'gradual_load';
  
  switch(scenario) {
    case 'gradual_load':
      gradualLoadScenario();
      break;
    case 'spike_test':
      spikeTestScenario();
      break;
    case 'sustained_load':
      sustainedLoadScenario();
      break;
    case 'memory_leak':
      memoryLeakTestScenario();
      break;
    default:
      gradualLoadScenario();
  }
}

// 점진적 부하 증가 시나리오
function gradualLoadScenario() {
  // 1. 사용자 등록/로그인
  if (!authToken) {
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
    
    if (registerRes.status === 201 || registerRes.status === 409) {
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
  }
  
  // 2. 다양한 API 호출로 부하 생성
  let requests = {
    'products_list': {
      method: 'GET',
      url: `${BASE_URL}/api/products`,
    },
    'product_detail': {
      method: 'GET',
      url: `${BASE_URL}/api/products/${Math.floor(Math.random() * 10) + 1}`,
    },
    'product_search': {
      method: 'GET',
      url: `${BASE_URL}/api/products/search?keyword=테스트`,
    },
    'category_products': {
      method: 'GET',
      url: `${BASE_URL}/api/products/category/1`,
    },
  };
  
  // 3. 동시에 여러 API 호출
  let responses = http.batch(requests);
  
  // 4. 응답 검증
  let allSuccess = true;
  for (let key in responses) {
    let success = check(responses[key], {
      [`${key} status is 200`]: (r) => r.status === 200,
      [`${key} response time < 5s`]: (r) => r.timings.duration < 5000,
    });
    
    if (!success) {
      allSuccess = false;
      if (responses[key].status === 0) {
        timeoutRate.add(1);
      } else if (responses[key].status >= 500) {
        circuitBreakerRate.add(1);
      }
    }
  }
  
  // 5. 인증이 필요한 API 테스트
  if (authToken && Math.random() < 0.3) {
    let profileRes = http.get(`${BASE_URL}/api/users/profile`, {
      headers: { 'Authorization': `Bearer ${authToken}` },
    });
    
    let profileSuccess = check(profileRes, {
      'user profile loaded': (r) => r.status === 200,
    });
    
    if (!profileSuccess) {
      allSuccess = false;
    }
  }
  
  // 6. 주문 생성 (부하 증가)
  if (authToken && Math.random() < 0.1) {
    let orderRes = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
      orderItems: [{
        productId: Math.floor(Math.random() * 10) + 1,
        quantity: Math.floor(Math.random() * 3) + 1,
        unitPrice: 10000,
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
    });
    
    check(orderRes, {
      'order creation handled': (r) => r.status === 201 || r.status === 400 || r.status === 500,
    });
  }
  
  if (allSuccess) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(Math.random() * 2 + 0.5); // 0.5-2.5초 랜덤 대기
}

// 스파이크 테스트 시나리오
function spikeTestScenario() {
  // 갑작스러운 트래픽 증가에 대한 시스템 반응 테스트
  let actions = [
    () => http.get(`${BASE_URL}/api/products`),
    () => http.get(`${BASE_URL}/api/products/${Math.floor(Math.random() * 10) + 1}`),
    () => http.get(`${BASE_URL}/api/products/search?keyword=phone`),
  ];
  
  let randomAction = actions[Math.floor(Math.random() * actions.length)];
  let res = randomAction();
  
  let success = check(res, {
    'spike test request successful': (r) => r.status === 200,
    'spike test response time < 10s': (r) => r.timings.duration < 10000,
  });
  
  if (success) {
    successRate.add(1);
  } else {
    errorRate.add(1);
    if (res.status === 0) {
      timeoutRate.add(1);
    } else if (res.status >= 500) {
      circuitBreakerRate.add(1);
    }
  }
  
  sleep(0.1); // 매우 짧은 대기로 빠른 반복
}

// 지속적 고부하 시나리오
function sustainedLoadScenario() {
  // 지속적인 고부하 상태에서의 시스템 안정성 테스트
  let requests = {
    'products_list': {
      method: 'GET',
      url: `${BASE_URL}/api/products`,
    },
    'product_detail': {
      method: 'GET',
      url: `${BASE_URL}/api/products/${Math.floor(Math.random() * 10) + 1}`,
    },
  };
  
  let responses = http.batch(requests);
  
  let allSuccess = true;
  for (let key in responses) {
    let success = check(responses[key], {
      [`${key} status is 200`]: (r) => r.status === 200,
    });
    
    if (!success) {
      allSuccess = false;
    }
  }
  
  if (allSuccess) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(1); // 1초 대기
}

// 메모리 누수 테스트 시나리오
function memoryLeakTestScenario() {
  // 장시간 실행으로 메모리 누수 확인
  let res = http.get(`${BASE_URL}/api/products`);
  
  let success = check(res, {
    'memory leak test successful': (r) => r.status === 200,
  });
  
  if (success) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(2); // 2초 대기
}

export function teardown(data) {
  console.log('시스템 한계 테스트 정리 중...');
  // 테스트 후 정리 작업이 필요한 경우
}
