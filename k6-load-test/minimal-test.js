import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

export default function() {
  console.log('=== 최소 테스트 시작 ===');
  
  // 1. Gateway 기본 접근
  let gatewayRes = http.get(`${BASE_URL}/`);
  console.log(`Gateway root: Status ${gatewayRes.status}`);
  
  // 2. Product Service 헬스체크
  let productHealthRes = http.get(`${BASE_URL}/api/products/health`);
  console.log(`Product health: Status ${productHealthRes.status}`);
  console.log(`Product health body: ${productHealthRes.body}`);
  
  // 3. 간단한 상품 목록 조회 (에러 상세 확인)
  let productsRes = http.get(`${BASE_URL}/api/products`);
  console.log(`Products: Status ${productsRes.status}`);
  console.log(`Products body: ${productsRes.body}`);
  
  // 4. 존재하지 않는 상품 조회
  let notFoundRes = http.get(`${BASE_URL}/api/products/999999`);
  console.log(`Not found product: Status ${notFoundRes.status}`);
  
  console.log('=== 최소 테스트 완료 ===');
  
  // 체크
  check(productHealthRes, {
    'product service health check': (r) => r.status === 200,
  });
}
