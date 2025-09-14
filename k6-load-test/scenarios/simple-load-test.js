import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 간단한 부하 테스트 시나리오
export let errorRate = new Rate('errors');

export let options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },   // 30초 동안 50명으로 증가
        { duration: '1m', target: 100 },   // 1분 동안 100명 유지
        { duration: '30s', target: 200 },  // 30초 동안 200명으로 증가
        { duration: '2m', target: 200 },   // 2분 동안 200명 유지
        { duration: '30s', target: 0 },    // 30초 동안 0명으로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.1'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080';
let authToken = '';
let userId = Math.floor(Math.random() * 1000) + 1;

export default function() {
  // 1. 사용자 등록
  if (!authToken) {
    let registerRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username: `loadtest_user${userId}`,
      email: `loadtest${userId}@example.com`,
      password: 'password123',
      fullName: `Load Test User ${userId}`
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    // 2. 로그인
    let loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      username: `loadtest_user${userId}`,
      password: 'password123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (loginRes.status === 200) {
      authToken = loginRes.json('token');
    }
  }
  
  // 3. 상품 목록 조회
  let productsRes = http.get(`${BASE_URL}/api/products`);
  check(productsRes, {
    'products list status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  
  // 4. 특정 상품 조회
  let productRes = http.get(`${BASE_URL}/api/products/1`);
  check(productRes, {
    'product detail status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  
  // 5. 인증이 필요한 API 테스트 (사용자 정보 조회)
  if (authToken) {
    let userRes = http.get(`${BASE_URL}/api/users/profile`, {
      headers: { 'Authorization': `Bearer ${authToken}` },
    });
    
    check(userRes, {
      'user profile status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  }
  
  sleep(1);
}