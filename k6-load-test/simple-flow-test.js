import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  scenarios: {
    // 전체 플로우를 순차적으로 실행
    complete_flow: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 1,
      maxDuration: '60s',
    }
  },
  
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080';

// 전역 변수
let authToken = '';
let userId = '';
let productId = '';
let orderId = '';

export default function() {
  // 1. 사용자 등록
  userRegistrationScenario();
  sleep(2); // 데이터베이스 트랜잭션 완료 대기
  
  // 2. 상품 등록
  productRegistrationScenario();
  sleep(1);
  
  // 3. 로그인
  loginScenario();
  sleep(1);
  
  // 4. 주문 생성
  orderScenario();
  sleep(1);
  
  // 5. 결제 처리
  paymentScenario();
}

// 1. 사용자 등록
function userRegistrationScenario() {
  let uniqueId = Date.now() + Math.floor(Math.random() * 1000);
  
  let registerRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
    username: `user${uniqueId}`,
    email: `user${uniqueId}@example.com`,
    password: 'password123',
    firstName: 'Test',
    lastName: 'User',
    phoneNumber: `010-${String(uniqueId).slice(-4).padStart(4, '0')}-${String(uniqueId).slice(-4).padStart(4, '0')}`,
    address: '서울시 강남구'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  console.log(`User Registration: ${registerRes.status} - user${uniqueId}`);
  
  check(registerRes, {
    'user registration successful': (r) => r.status === 201 || r.status === 409,
  });
  
  userId = uniqueId;
  sleep(0.1);
}

// 2. 상품 등록
function productRegistrationScenario() {
  let uniqueId = Date.now() + Math.floor(Math.random() * 1000);
  
  let productRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
    name: `테스트상품${uniqueId}`,
    description: `테스트 상품 설명 ${uniqueId}`,
    price: Math.floor(Math.random() * 100000) + 10000,
    stock: Math.floor(Math.random() * 100) + 10,
    category: '테스트카테고리',
    imageUrl: 'https://example.com/image.jpg'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  console.log(`Product Registration: ${productRes.status} - 테스트상품${uniqueId}`);
  
  check(productRes, {
    'product registration successful': (r) => r.status === 201,
  });
  
  sleep(0.1);
}

// 3. 로그인
function loginScenario() {
  let loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    usernameOrEmail: `user${userId}`,
    password: 'password123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  console.log(`Login: ${loginRes.status} - user${userId}`);
  console.log(`Login Response Body: ${loginRes.body}`);
  
  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });
  
  if (loginRes.status === 200) {
    let loginData = JSON.parse(loginRes.body);
    authToken = loginData.token;
    console.log(`Auth Token: ${authToken}`);
  } else {
    console.log(`Login failed for user${userId}: ${loginRes.status} - ${loginRes.body}`);
  }
  
  sleep(0.1);
}

// 4. 주문 생성
function orderScenario() {
  if (!authToken) {
    console.log('Missing auth token');
    return;
  }
  
  // 1. 상품 목록 조회
  let productsRes = http.get(`${BASE_URL}/api/products`, {
    headers: { 
      'Authorization': `Bearer ${authToken}`
    },
  });
  
  console.log(`Products List: ${productsRes.status}`);
  
  if (productsRes.status !== 200) {
    console.log('Failed to get products list');
    return;
  }
  
  let products = JSON.parse(productsRes.body);
  if (!products || products.length === 0) {
    console.log('No products available');
    return;
  }
  
  // 2. 첫 번째 상품 선택
  let selectedProduct = products[0];
  productId = selectedProduct.id;
  
  console.log(`Selected Product: ${selectedProduct.name} (ID: ${productId})`);
  
  // 3. 주문 생성
  let orderRes = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
    orderItems: [{
      productId: productId,
      quantity: Math.floor(Math.random() * 3) + 1
    }]
  }), {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
  });
  
  console.log(`Order Creation: ${orderRes.status} - Product: ${productId}`);
  
  check(orderRes, {
    'order creation successful': (r) => r.status === 201,
  });
  
  if (orderRes.status === 201) {
    let orderData = JSON.parse(orderRes.body);
    orderId = orderData.id;
  }
  
  sleep(0.1);
}

// 5. 결제 처리
function paymentScenario() {
  if (!authToken || !orderId) {
    console.log('Missing auth token or order ID');
    return;
  }
  
  let paymentRes = http.post(`${BASE_URL}/api/payments`, JSON.stringify({
    orderId: orderId,
    paymentMethod: 'CARD',
    amount: Math.floor(Math.random() * 100000) + 10000
  }), {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
  });
  
  console.log(`Payment: ${paymentRes.status} - Order: ${orderId}`);
  
  check(paymentRes, {
    'payment successful': (r) => r.status === 200 || r.status === 201,
  });
  
  sleep(0.1);
}
