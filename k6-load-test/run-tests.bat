@echo off
echo ================================
echo Flash Sale MSA Load Testing
echo ================================
echo.

:: K6 설치 확인
k6 version >nul 2>&1
if %errorlevel% neq 0 (
    echo K6가 설치되지 않았습니다. 다음 링크에서 설치하세요:
    echo https://k6.io/docs/get-started/installation/
    echo.
    echo 또는 다음 명령어로 설치:
    echo winget install k6
    pause
    exit /b 1
)

echo K6가 설치되어 있습니다.
echo.

:menu
echo 테스트 시나리오를 선택하세요:
echo 1. 간단한 부하 테스트 (Simple Load Test)
echo 2. 스트레스 테스트 (Stress Test)
echo 3. 플래시 세일 시나리오 테스트 (Flash Sale Scenario)
echo 4. 모든 테스트 순차 실행
echo 5. 종료
echo.

set /p choice=선택 (1-5): 

if "%choice%"=="1" goto simple_test
if "%choice%"=="2" goto stress_test
if "%choice%"=="3" goto flash_sale_test
if "%choice%"=="4" goto all_tests
if "%choice%"=="5" goto end
echo 잘못된 선택입니다.
goto menu

:simple_test
echo.
echo === 간단한 부하 테스트 실행 ===
k6 run scenarios\simple-load-test.js
goto menu

:stress_test
echo.
echo === 스트레스 테스트 실행 ===
k6 run scenarios\stress-test.js
goto menu

:flash_sale_test
echo.
echo === 플래시 세일 시나리오 테스트 실행 ===
k6 run flash-sale-test.js
goto menu

:all_tests
echo.
echo === 모든 테스트 순차 실행 ===
echo.
echo 1단계: 간단한 부하 테스트
k6 run scenarios\simple-load-test.js

echo.
echo 2단계: 스트레스 테스트
timeout /t 10 /nobreak
k6 run scenarios\stress-test.js

echo.
echo 3단계: 플래시 세일 시나리오 테스트
timeout /t 10 /nobreak
k6 run flash-sale-test.js

echo.
echo === 모든 테스트 완료 ===
goto menu

:end
echo.
echo 테스트를 종료합니다.
pause