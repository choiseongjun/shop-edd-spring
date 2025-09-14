#!/bin/bash

echo "================================"
echo "Flash Sale MSA Load Testing"
echo "================================"
echo

# K6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "K6가 설치되지 않았습니다. 다음 링크에서 설치하세요:"
    echo "https://k6.io/docs/get-started/installation/"
    echo
    echo "또는 다음 명령어로 설치:"
    echo "# Ubuntu/Debian"
    echo "sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69"
    echo "echo 'deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main' | sudo tee /etc/apt/sources.list.d/k6.list"
    echo "sudo apt-get update && sudo apt-get install k6"
    echo
    echo "# macOS"
    echo "brew install k6"
    exit 1
fi

echo "K6가 설치되어 있습니다."
echo

while true; do
    echo "테스트 시나리오를 선택하세요:"
    echo "1. 간단한 부하 테스트 (Simple Load Test)"
    echo "2. 스트레스 테스트 (Stress Test)"
    echo "3. 플래시 세일 시나리오 테스트 (Flash Sale Scenario)"
    echo "4. 모든 테스트 순차 실행"
    echo "5. 종료"
    echo

    read -p "선택 (1-5): " choice

    case $choice in
        1)
            echo
            echo "=== 간단한 부하 테스트 실행 ==="
            k6 run scenarios/simple-load-test.js
            ;;
        2)
            echo
            echo "=== 스트레스 테스트 실행 ==="
            k6 run scenarios/stress-test.js
            ;;
        3)
            echo
            echo "=== 플래시 세일 시나리오 테스트 실행 ==="
            k6 run flash-sale-test.js
            ;;
        4)
            echo
            echo "=== 모든 테스트 순차 실행 ==="
            echo
            echo "1단계: 간단한 부하 테스트"
            k6 run scenarios/simple-load-test.js

            echo
            echo "2단계: 스트레스 테스트"
            sleep 10
            k6 run scenarios/stress-test.js

            echo
            echo "3단계: 플래시 세일 시나리오 테스트"
            sleep 10
            k6 run flash-sale-test.js

            echo
            echo "=== 모든 테스트 완료 ==="
            ;;
        5)
            echo
            echo "테스트를 종료합니다."
            exit 0
            ;;
        *)
            echo "잘못된 선택입니다."
            ;;
    esac
    echo
done