@echo off
echo ========================================
echo K6 개별 테스트 실행
echo ========================================

if "%1"=="" (
    echo 사용법: run-individual-tests.bat [테스트명]
    echo.
    echo 사용 가능한 테스트:
    echo   comprehensive - 종합 플로우 테스트
    echo   flash-sale    - 플래시 세일 스트레스 테스트
    echo   system-limits - 시스템 한계 테스트
    echo   api-specific  - API별 개별 테스트
    echo   simple        - 간단한 부하 테스트
    echo   stress        - 스트레스 테스트
    echo.
    pause
    exit /b 1
)

if "%1"=="comprehensive" (
    echo 종합 플로우 테스트 실행 중...
    k6 run comprehensive-flow-test.js
) else if "%1"=="flash-sale" (
    echo 플래시 세일 스트레스 테스트 실행 중...
    k6 run flash-sale-stress-test.js
) else if "%1"=="system-limits" (
    echo 시스템 한계 테스트 실행 중...
    k6 run system-limits-test.js
) else if "%1"=="api-specific" (
    echo API별 개별 테스트 실행 중...
    k6 run api-specific-tests.js
) else if "%1"=="simple" (
    echo 간단한 부하 테스트 실행 중...
    k6 run scenarios/simple-load-test.js
) else if "%1"=="stress" (
    echo 스트레스 테스트 실행 중...
    k6 run scenarios/stress-test.js
) else (
    echo 알 수 없는 테스트명: %1
    echo 사용 가능한 테스트를 확인하세요.
    pause
    exit /b 1
)

echo.
echo 테스트 완료!
pause

