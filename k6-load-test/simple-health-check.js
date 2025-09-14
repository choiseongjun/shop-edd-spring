import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

export default function() {
  console.log('=== 서비스 헬스체크 시작 ===');
  
  // 1. Gateway 헬스체크
  let gatewayHealthRes = http.get(`${BASE_URL}/actuator/health`);
  console.log(`Gateway health: Status ${gatewayHealthRes.status}`);
  if (gatewayHealthRes.status !== 200) {
    console.log(`Gateway health body: ${gatewayHealthRes.body}`);
  }
  
  // 2. Product Service 헬스체크
  let productHealthRes = http.get(`${BASE_URL}/api/products/health`);
  console.log(`Product service health: Status ${productHealthRes.status}`);
  if (productHealthRes.status !== 200) {
    console.log(`Product health body: ${productHealthRes.body}`);
  }
  
  // 3. User Service 헬스체크
  let userHealthRes = http.get(`${BASE_URL}/api/users/health`);
  console.log(`User service health: Status ${userHealthRes.status}`);
  if (userHealthRes.status !== 200) {
    console.log(`User health body: ${userHealthRes.body}`);
  }
  
  // 4. Order Service 헬스체크
  let orderHealthRes = http.get(`${BASE_URL}/api/orders/health`);
  console.log(`Order service health: Status ${orderHealthRes.status}`);
  if (orderHealthRes.status !== 200) {
    console.log(`Order health body: ${orderHealthRes.body}`);
  }
  
  // 5. Payment Service 헬스체크
  let paymentHealthRes = http.get(`${BASE_URL}/api/payments/health`);
  console.log(`Payment service health: Status ${paymentHealthRes.status}`);
  if (paymentHealthRes.status !== 200) {
    console.log(`Payment health body: ${paymentHealthRes.body}`);
  }
  
  // 6. 상품 목록 조회
  let productsRes = http.get(`${BASE_URL}/api/products`);
  console.log(`Products list: Status ${productsRes.status}`);
  console.log(`Products list body: ${productsRes.body}`);
  
  // 7. 상품 검색 테스트
  let searchRes = http.get(`${BASE_URL}/api/products/search?keyword=test`);
  console.log(`Product search: Status ${searchRes.status}`);
  console.log(`Product search body: ${searchRes.body}`);
  
  // 8. 카테고리별 상품 조회
  let categoryRes = http.get(`${BASE_URL}/api/products/category/1`);
  console.log(`Category products: Status ${categoryRes.status}`);
  console.log(`Category products body: ${categoryRes.body}`);
  
  console.log('=== 서비스 헬스체크 완료 ===');
  
  // 기본 체크
  check(gatewayHealthRes, {
    'gateway is up': (r) => r.status === 200,
  });
  
  check(productHealthRes, {
    'product service is up': (r) => r.status === 200,
  });
  
  check(productsRes, {
    'products endpoint works': (r) => r.status === 200,
  });
}
