@echo off
echo ========================================
echo K6 종합 부하 테스트 실행
echo ========================================

echo.
echo 1. 종합 플로우 테스트 실행 중...
k6 run comprehensive-flow-test.js

echo.
echo 2. 플래시 세일 스트레스 테스트 실행 중...
k6 run flash-sale-stress-test.js

echo.
echo 3. 시스템 한계 테스트 실행 중...
k6 run system-limits-test.js

echo.
echo 4. API별 개별 테스트 실행 중...
k6 run api-specific-tests.js

echo.
echo ========================================
echo 모든 테스트 완료!
echo ========================================
pause
