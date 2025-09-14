import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8080';

export default function() {
  // 1. 사용자 등록
  let uniqueId = Date.now() + Math.floor(Math.random() * 1000);
  
  console.log(`=== 사용자 등록 시작: user${uniqueId} ===`);
  
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
  
  console.log(`회원가입 응답: ${registerRes.status}`);
  console.log(`회원가입 응답 본문: ${registerRes.body}`);
  
  check(registerRes, {
    'user registration successful': (r) => r.status === 201 || r.status === 409,
  });
  
  // 2. 5초 대기
  console.log('5초 대기 중...');
  sleep(5);
  
  // 3. 로그인 시도
  console.log(`=== 로그인 시도: user${uniqueId} ===`);
  
  let loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    usernameOrEmail: `user${uniqueId}`,
    password: 'password123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  console.log(`로그인 응답: ${loginRes.status}`);
  console.log(`로그인 응답 본문: ${loginRes.body}`);
  
  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });
}
