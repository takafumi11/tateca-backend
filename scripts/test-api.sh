#!/bin/bash

# ============================================================================
# Tateca Backend API Test Script
# Usage: ./scripts/test-api.sh
# Prerequisites:
#   - Application running on http://localhost:8080 with dev profile
#   - Database initialized with test data (see init-test-data.sql)
#   - jq installed (optional, for JSON formatting)
# ============================================================================

set -e

# Configuration
BASE_URL="http://localhost:8080"
USE_COLOR=true
USE_JQ=true

# Test tracking
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0

# Check if jq is available
if ! command -v jq &> /dev/null; then
    USE_JQ=false
    echo "Warning: jq not found. JSON output will not be formatted."
fi

# Color codes
if [ "$USE_COLOR" = true ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    CYAN='\033[0;36m'
    NC='\033[0m' # No Color
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    CYAN=''
    NC=''
fi

# Test data UUIDs
TEST_USER_1="test-user-001"
TEST_USER_2="test-user-002"
TEST_USER_3="test-user-003"
USER_UUID_1="550e8400-e29b-41d4-a716-446655440001"
USER_UUID_2="550e8400-e29b-41d4-a716-446655440002"
USER_UUID_3="550e8400-e29b-41d4-a716-446655440003"
GROUP_UUID_1="650e8400-e29b-41d4-a716-446655440001"
JOIN_TOKEN_1="750e8400-e29b-41d4-a716-446655440001"
TRANSACTION_UUID_1="850e8400-e29b-41d4-a716-446655440001"

# Helper functions
print_header() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_test() {
    echo -e "\n${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ Success${NC}"
}

print_error() {
    echo -e "${RED}✗ Error: $1${NC}"
}

format_json() {
    if [ "$USE_JQ" = true ]; then
        jq . 2>/dev/null || cat
    else
        cat
    fi
}

test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    shift 3

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    print_test "$description"
    echo -e "${YELLOW}$method $endpoint${NC}"

    local response
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" "$@")

    # macOS compatible: use sed instead of head -n -1
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)

    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        print_success
        echo "$body" | format_json
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "HTTP $status"
        echo "$body" | format_json
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi

    return 0
}

# Main test execution
main() {
    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║        Tateca Backend API Test Suite                  ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    echo -e "Base URL: ${YELLOW}$BASE_URL${NC}"
    echo -e "Date: ${YELLOW}$(date)${NC}"

    # ========================================
    # 0. Setup: Initialize Database
    # ========================================
    print_header "0. Setup: Initialize Test Data"

    echo -e "${BLUE}▶ Cleaning and loading test data into database${NC}"
    if MYSQL_PWD=a mysql -h 127.0.0.1 -P 3306 -u a db < scripts/init-test-data.sql 2>/dev/null; then
        echo -e "${GREEN}✓ Test data loaded successfully${NC}"
    else
        echo -e "${RED}✗ Failed to load test data${NC}"
        echo -e "${YELLOW}Make sure MySQL is running: docker compose up -d${NC}"
        echo -e "${YELLOW}Try: MYSQL_PWD=a mysql -h 127.0.0.1 -P 3306 -u a db < scripts/init-test-data.sql${NC}"
        exit 1
    fi

    echo -e "${BLUE}▶ Fetching exchange rates from external API${NC}"
    local exchange_response
    exchange_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/internal/exchange-rates" -H "X-API-Key: dev-api-key-local")
    local exchange_status=$(echo "$exchange_response" | tail -n 1)
    if [ "$exchange_status" -ge 200 ] && [ "$exchange_status" -lt 300 ]; then
        echo -e "${GREEN}✓ Exchange rates populated successfully${NC}"
    else
        echo -e "${RED}✗ Failed to fetch exchange rates (HTTP $exchange_status)${NC}"
        echo -e "${YELLOW}Warning: Exchange rate tests may fail${NC}"
    fi

    # ========================================
    # 1. DevController Tests
    # ========================================
    print_header "1. DevController Tests (/dev)"

    test_endpoint GET "/dev/firebase-token/$TEST_USER_1" \
        "1.1 Generate Firebase Custom Token"

    # ========================================
    # 2. AuthUserController Tests
    # ========================================
    print_header "2. AuthUserController Tests (/auth/users)"

    test_endpoint GET "/auth/users/$TEST_USER_1" \
        "2.1 Get Auth User" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint POST "/auth/users" \
        "2.2 Create Auth User (will likely fail - user already exists)" \
        -H "Content-Type: application/json" \
        -H "x-uid: test-user-004" \
        -d '{
            "name": "New User",
            "email": "newuser@example.com"
        }'

    test_endpoint PATCH "/auth/users/review-preferences" \
        "2.3 Update App Review Preferences" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_1" \
        -d '{
            "show_dialog": false,
            "app_review_status": "COMPLETED"
        }'

    test_endpoint DELETE "/auth/users/test-user-004" \
        "2.4 Delete Auth User (will likely fail - user does not exist)" \
        -H "x-uid: $TEST_USER_1"

    # ========================================
    # 3. UserController Tests
    # ========================================
    print_header "3. UserController Tests (/users)"

    test_endpoint PATCH "/users/$USER_UUID_1" \
        "3.1 Update User Name" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_1" \
        -d '{
            "user_name": "Alice Updated"
        }'

    # ========================================
    # 4. GroupController Tests
    # ========================================
    print_header "4. GroupController Tests (/groups)"

    test_endpoint GET "/groups/$GROUP_UUID_1" \
        "4.1 Get Group Info" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint GET "/groups/list" \
        "4.2 Get Group List" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint POST "/groups" \
        "4.3 Create Group" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_1" \
        -d '{
            "group_name": "Summer Trip 2025",
            "host_name": "Alice",
            "participants_name": ["Bob", "Charlie"]
        }'

    test_endpoint PATCH "/groups/$GROUP_UUID_1" \
        "4.4 Update Group Name" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_1" \
        -d '{
            "group_name": "Team Outing 2025 Updated"
        }'

    test_endpoint POST "/groups/$GROUP_UUID_1" \
        "4.5 Join Group (with invitation token)" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_3" \
        -d "{
            \"user_uuid\": \"$USER_UUID_3\",
            \"join_token\": \"$JOIN_TOKEN_1\"
        }"

    test_endpoint DELETE "/groups/$GROUP_UUID_1/users/$USER_UUID_2" \
        "4.6 Remove User from Group" \
        -H "x-uid: $TEST_USER_1"

    # ========================================
    # 5. TransactionController Tests
    # ========================================
    print_header "5. TransactionController Tests (/groups/{groupId}/transactions)"

    test_endpoint GET "/groups/$GROUP_UUID_1/transactions/history?count=10" \
        "5.1 Get Transaction History" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint GET "/groups/$GROUP_UUID_1/transactions/settlement" \
        "5.2 Get Transaction Settlement (no currency)" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint GET "/groups/$GROUP_UUID_1/transactions/settlement?currencyCode=JPY" \
        "5.3 Get Transaction Settlement (JPY)" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint GET "/groups/$GROUP_UUID_1/transactions/$TRANSACTION_UUID_1" \
        "5.4 Get Transaction Detail" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint POST "/groups/$GROUP_UUID_1/transactions" \
        "5.5 Create Transaction (LOAN)" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_1" \
        -d "{
            \"transaction_type\": \"LOAN\",
            \"title\": \"Lunch at cafe\",
            \"amount\": 3000,
            \"currency_code\": \"JPY\",
            \"date_str\": \"$(date +%Y-%m-%d)T09:00:00+09:00\",
            \"payer_id\": \"$USER_UUID_1\",
            \"loan\": {
                \"obligations\": [
                    {
                        \"user_uuid\": \"$USER_UUID_2\",
                        \"amount\": 1500
                    }
                ]
            }
        }"

    test_endpoint POST "/groups/$GROUP_UUID_1/transactions" \
        "5.6 Create Transaction (REPAYMENT)" \
        -H "Content-Type: application/json" \
        -H "x-uid: $TEST_USER_2" \
        -d "{
            \"transaction_type\": \"REPAYMENT\",
            \"title\": \"Repay Alice\",
            \"amount\": 1000,
            \"currency_code\": \"JPY\",
            \"date_str\": \"$(date +%Y-%m-%d)T09:00:00+09:00\",
            \"payer_id\": \"$USER_UUID_2\",
            \"repayment\": {
                \"recipient_id\": \"$USER_UUID_1\"
            }
        }"

    test_endpoint DELETE "/groups/$GROUP_UUID_1/transactions/$TRANSACTION_UUID_1" \
        "5.7 Delete Transaction" \
        -H "x-uid: $TEST_USER_1"

    # ========================================
    # 6. ExchangeRateController Tests
    # ========================================
    print_header "6. ExchangeRateController Tests (/exchange-rate)"

    test_endpoint GET "/exchange-rate/$(date +%Y-%m-%d)" \
        "6.1 Get Exchange Rates (Today)" \
        -H "x-uid: $TEST_USER_1"

    test_endpoint GET "/exchange-rate/$(date +%Y-%m-%d)" \
        "6.2 Get Exchange Rates (Same Date as Test Data)" \
        -H "x-uid: $TEST_USER_1"

    # ========================================
    # 8. ExchangeRateLambdaController Tests
    # ========================================
    print_header "8. ExchangeRateLambdaController Tests (/internal/exchange-rates)"

    test_endpoint POST "/internal/exchange-rates" \
        "8.1 Trigger Exchange Rate Update (already done in setup)" \
        -H "X-API-Key: dev-api-key-local"

    # ========================================
    # Summary
    # ========================================
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}Test Summary${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "Total Tests:  ${YELLOW}$TESTS_TOTAL${NC}"
    echo -e "Passed:       ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed:       ${RED}$TESTS_FAILED${NC}"
    echo ""

    if [ "$TESTS_FAILED" -eq 0 ]; then
        echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║        All Tests Passed Successfully! ✓               ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    else
        echo -e "${RED}╔════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║          Some Tests Failed ✗                          ║${NC}"
        echo -e "${RED}╚════════════════════════════════════════════════════════╝${NC}"
        echo -e "${YELLOW}Note: Check individual test results above for details.${NC}"
    fi
}

# Run main function
main "$@"
