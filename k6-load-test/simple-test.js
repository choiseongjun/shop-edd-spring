import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 1,
  duration: '10s',
};

export default function() {
  // 1. Gateway Health Check
  let healthRes = http.get('http://localhost:8080/actuator/health');
  console.log('Gateway Health:', healthRes.status);
  
  // 2. User Registration Test
  let registerRes = http.post('http://localhost:8080/api/auth/register', JSON.stringify({
    username: `testuser${Date.now()}`,
    email: `test${Date.now()}@example.com`,
    password: 'password123',
    firstName: 'Test',
    lastName: 'User',
    phoneNumber: '010-1234-5678',
    address: '서울시 강남구'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  console.log('Registration Status:', registerRes.status);
  console.log('Registration Body:', registerRes.body);
  
  check(registerRes, {
    'registration successful': (r) => r.status === 201 || r.status === 409,
  });
  
  sleep(1);
}
