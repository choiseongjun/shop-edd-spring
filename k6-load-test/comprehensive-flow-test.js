import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');

// 테스트 설정
export let options = {
  scenarios: {
    // 시나리오 1: 사용자 등록 및 로그인
    user_registration: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 1,
      startTime: '0s',
      maxDuration: '30s',
      tags: { scenario: 'registration' },
    },
    
    // 시나리오 2: 일반적인 쇼핑 플로우
    shopping_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },   // 1분 동안 20명으로 증가
        { duration: '3m', target: 50 },   // 3분 동안 50명으로 증가
        { duration: '2m', target: 100 },  // 2분 동안 100명으로 증가
        { duration: '2m', target: 50 },   // 2분 동안 50명으로 감소
        { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
      ],
      startTime: '30s',
      tags: { scenario: 'shopping' },
    },
    
    // 시나리오 3: 플래시 세일 테스트
    flash_sale: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: 200 },  // 30초 동안 초당 200 요청
        { duration: '1m', target: 500 },   // 1분 동안 초당 500 요청
        { duration: '30s', target: 1000 }, // 30초 동안 초당 1000 요청 (폭발적 접근)
        { duration: '1m', target: 200 },   // 1분 동안 초당 200 요청으로 감소
      ],
      preAllocatedVUs: 200,
      maxVUs: 2000,
      startTime: '8m30s',
      tags: { scenario: 'flash_sale' },
    },
  },
  
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이내
    http_req_failed: ['rate<0.1'],     // 실패율 10% 이하
    errors: ['rate<0.15'],             // 에러율 15% 이하
    success: ['rate>0.85'],            // 성공율 85% 이상
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
  console.log('테스트 환경 설정 시작...');
  
  // 관리자 계정 생성 및 로그인
  let adminRegisterRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
    username: 'admin',
    email: 'admin@shop.com',
    password: 'admin123',
    firstName: 'Admin',
    lastName: 'User',
    phoneNumber: '010-0000-0000',
    address: '서울시 강남구'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  let adminToken = '';
  if (adminRegisterRes.status === 200 || adminRegisterRes.status === 409) {
    let adminLoginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      usernameOrEmail: 'admin',
      password: 'admin123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (adminLoginRes.status === 200) {
      adminToken = adminLoginRes.json('token');
    }
  }
  
  // 테스트용 상품 생성
  if (adminToken) {
    let createProductRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
      name: '테스트 상품',
      description: 'k6 테스트용 상품입니다.',
      price: 100000,
      originalPrice: 150000,
      stock: 1000,
      categoryId: 1,
      imageUrl: 'https://example.com/product.jpg',
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
      console.log(`테스트 상품 생성됨: ID ${productId}`);
    }
    
    // 플래시 세일 상품 생성
    let createFlashSaleRes = http.post(`${BASE_URL}/api/products`, JSON.stringify({
      name: '플래시 세일 상품',
      description: '한정 시간 특가 상품!',
      price: 50000,
      originalPrice: 100000,
      stock: 100,
      categoryId: 1,
      imageUrl: 'https://example.com/flash-sale.jpg',
      active: true,
      flashSale: true,
      flashSaleStartTime: new Date(Date.now() + 8 * 60 * 1000).toISOString(), // 8분 후 시작
      flashSaleEndTime: new Date(Date.now() + 12 * 60 * 1000).toISOString(),   // 12분 후 종료
      flashSaleStock: 100,
      discountRate: 50
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
    });
    
    if (createFlashSaleRes.status === 201) {
      console.log(`플래시 세일 상품 생성됨: ID ${createFlashSaleRes.json('id')}`);
    }
  }
  
  return { adminToken: adminToken, productId: productId };
}

export default function(data) {
  const scenario = __ENV.SCENARIO || 'shopping';
  
  switch(scenario) {
    case 'registration':
      registrationScenario();
      break;
    case 'shopping':
      shoppingScenario(data);
      break;
    case 'flash_sale':
      flashSaleScenario(data);
      break;
    default:
      shoppingScenario(data);
  }
}

// 사용자 등록 시나리오
function registrationScenario() {
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
  
  let success = check(registerRes, {
    'user registration successful': (r) => r.status === 201 || r.status === 409,
  });
  
  if (success) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(0.5);
}

// 일반 쇼핑 플로우 시나리오
function shoppingScenario(data) {
  // 1. 로그인
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
  
  // 2. 상품 목록 조회
  let productsRes = http.get(`${BASE_URL}/api/products`);
  let productsSuccess = check(productsRes, {
    'products list loaded': (r) => r.status === 200,
  });
  
  // 3. 상품 상세 조회
  let productRes = http.get(`${BASE_URL}/api/products/${data.productId || 1}`);
  let productSuccess = check(productRes, {
    'product detail loaded': (r) => r.status === 200,
  });
  
  // 4. 상품 검색
  let searchRes = http.get(`${BASE_URL}/api/products/search?keyword=테스트`);
  let searchSuccess = check(searchRes, {
    'product search successful': (r) => r.status === 200,
  });
  
  // 5. 사용자 프로필 조회 (인증 필요)
  if (authToken) {
    let profileRes = http.get(`${BASE_URL}/api/users/profile`, {
      headers: { 'Authorization': `Bearer ${authToken}` },
    });
    let profileSuccess = check(profileRes, {
      'user profile loaded': (r) => r.status === 200,
    });
    
    // 6. 주문 생성 (랜덤하게)
    if (Math.random() < 0.3 && profileSuccess) { // 30% 확률로 주문
      let orderRes = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
        orderItems: [{
          productId: data.productId || 1,
          quantity: Math.floor(Math.random() * 3) + 1,
          unitPrice: 100000,
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
      
      let orderSuccess = check(orderRes, {
        'order created successfully': (r) => r.status === 201,
      });
      
      if (orderSuccess) {
        orderId = orderRes.json('order.orderId');
        
        // 7. 결제 처리 (주문 성공 시)
        if (orderId) {
          let paymentRes = http.post(`${BASE_URL}/api/payments/process`, JSON.stringify({
            orderId: orderId,
            amount: 100000,
            paymentMethod: 'CREDIT_CARD',
            flashSalePayment: false
          }), {
            headers: { 
              'Content-Type': 'application/json',
              'User-Id': userId.toString()
            },
          });
          
          check(paymentRes, {
            'payment processed successfully': (r) => r.status === 200,
          });
        }
      }
    }
  }
  
  // 전체 성공 여부 확인
  let overallSuccess = productsSuccess && productSuccess && searchSuccess;
  if (overallSuccess) {
    successRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(Math.random() * 2 + 1); // 1-3초 랜덤 대기
}

// 플래시 세일 시나리오
function flashSaleScenario(data) {
  // 1. 로그인
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
    // 2. 플래시 세일 상품 조회
    let flashSaleRes = http.get(`${BASE_URL}/api/products/flash-sale`);
    let flashSaleSuccess = check(flashSaleRes, {
      'flash sale products loaded': (r) => r.status === 200,
    });
    
    // 3. 재고 예약 시도
    let reserveRes = http.post(`${BASE_URL}/api/products/reserve-stock`, JSON.stringify({
      productId: 2, // 플래시 세일 상품 ID (가정)
      quantity: 1,
      userId: userId,
      orderId: `FLASH_ORDER_${userId}_${Date.now()}`
    }), {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
    });
    
    let reserveSuccess = check(reserveRes, {
      'stock reservation attempted': (r) => r.status === 200 || r.status === 409,
      'stock reservation successful': (r) => r.status === 200,
    });
    
    // 4. 주문 생성 (재고 예약 성공 시)
    if (reserveSuccess && reserveRes.status === 200) {
      let orderRes = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
        orderItems: [{
          productId: 2,
          quantity: 1,
          unitPrice: 50000,
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
      
      // 5. 결제 처리
      if (orderSuccess) {
        let orderId = orderRes.json('order.orderId');
        let paymentRes = http.post(`${BASE_URL}/api/payments/process`, JSON.stringify({
          orderId: orderId,
          amount: 50000,
          paymentMethod: 'CREDIT_CARD',
          flashSalePayment: true
        }), {
          headers: { 
            'Content-Type': 'application/json',
            'User-Id': userId.toString()
          },
        });
        
        check(paymentRes, {
          'flash sale payment processed': (r) => r.status === 200,
        });
      }
    }
    
    // 전체 성공 여부 확인
    if (flashSaleSuccess) {
      successRate.add(1);
    } else {
      errorRate.add(1);
    }
  } else {
    errorRate.add(1);
  }
  
  sleep(0.1); // 짧은 대기로 빠른 반복
}

export function teardown(data) {
  console.log('테스트 정리 중...');
  // 테스트 후 정리 작업이 필요한 경우
}
