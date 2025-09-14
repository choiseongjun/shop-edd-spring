#!/bin/bash

echo "========================================"
echo "K6 개별 테스트 실행"
echo "========================================"

if [ -z "$1" ]; then
    echo "사용법: ./run-individual-tests.sh [테스트명]"
    echo ""
    echo "사용 가능한 테스트:"
    echo "  comprehensive - 종합 플로우 테스트"
    echo "  flash-sale    - 플래시 세일 스트레스 테스트"
    echo "  system-limits - 시스템 한계 테스트"
    echo "  api-specific  - API별 개별 테스트"
    echo "  simple        - 간단한 부하 테스트"
    echo "  stress        - 스트레스 테스트"
    echo ""
    exit 1
fi

case "$1" in
    "comprehensive")
        echo "종합 플로우 테스트 실행 중..."
        k6 run comprehensive-flow-test.js
        ;;
    "flash-sale")
        echo "플래시 세일 스트레스 테스트 실행 중..."
        k6 run flash-sale-stress-test.js
        ;;
    "system-limits")
        echo "시스템 한계 테스트 실행 중..."
        k6 run system-limits-test.js
        ;;
    "api-specific")
        echo "API별 개별 테스트 실행 중..."
        k6 run api-specific-tests.js
        ;;
    "simple")
        echo "간단한 부하 테스트 실행 중..."
        k6 run scenarios/simple-load-test.js
        ;;
    "stress")
        echo "스트레스 테스트 실행 중..."
        k6 run scenarios/stress-test.js
        ;;
    *)
        echo "알 수 없는 테스트명: $1"
        echo "사용 가능한 테스트를 확인하세요."
        exit 1
        ;;
esac

echo ""
echo "테스트 완료!"

