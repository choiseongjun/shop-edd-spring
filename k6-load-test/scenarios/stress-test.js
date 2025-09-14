import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 스트레스 테스트 - 시스템 한계 테스트
export let errorRate = new Rate('errors');

export let options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },   // 1분 동안 100명으로 증가
        { duration: '1m', target: 300 },   // 1분 동안 300명으로 증가
        { duration: '1m', target: 500 },   // 1분 동안 500명으로 증가
        { duration: '2m', target: 1000 },  // 2분 동안 1000명으로 증가 (스트레스)
        { duration: '1m', target: 1500 },  // 1분 동안 1500명으로 증가 (한계 테스트)
        { duration: '2m', target: 1500 },  // 2분 동안 1500명 유지
        { duration: '1m', target: 0 },     // 1분 동안 0명으로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],  // 스트레스 상황에서는 응답시간 기준 완화
    http_req_failed: ['rate<0.2'],      // 실패율 20% 이하
    errors: ['rate<0.3'],               // 에러율 30% 이하
  },
};

const BASE_URL = 'http://localhost:8080';
let userId = Math.floor(Math.random() * 10000) + 1;

export default function() {
  // 동시에 여러 API 호출하여 시스템 부하 증가
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
      url: `${BASE_URL}/api/products/search?keyword=phone`,
    },
  };
  
  let responses = http.batch(requests);
  
  // 모든 응답 체크
  for (let key in responses) {
    check(responses[key], {
      [`${key} status is 200`]: (r) => r.status === 200,
    }) || errorRate.add(1);
  }
  
  // 재고 예약 시도 (동시성 테스트)
  let reserveRes = http.post(`${BASE_URL}/api/products/reserve-stock`, JSON.stringify({
    productId: 1,
    quantity: 1,
    userId: userId
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(reserveRes, {
    'stock reservation handled': (r) => r.status === 200 || r.status === 409 || r.status === 400,
  }) || errorRate.add(1);
  
  sleep(0.5); // 짧은 대기로 부하 증가
}