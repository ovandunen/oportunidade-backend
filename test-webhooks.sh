#!/bin/bash

# AppyPay Webhook Test Script
# This script tests the webhook endpoint with various scenarios

BASE_URL="${BASE_URL:-http://localhost:8080}"
WEBHOOK_URL="$BASE_URL/webhooks/appypay"

echo "========================================"
echo "AppyPay Webhook Integration Test Script"
echo "========================================"
echo "Testing endpoint: $WEBHOOK_URL"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
curl -s -X GET "$WEBHOOK_URL/health" | jq .
echo ""

# Test 2: Successful Payment
echo -e "${YELLOW}Test 2: Successful Payment${NC}"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "tx-success-'$(date +%s)'",
    "merchantTransactionId": "ORDER-SUCCESS-'$(date +%s)'",
    "type": "Charge",
    "amount": 1500.00,
    "currency": "AOA",
    "status": "Success",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "123456789",
      "entity": "00123",
      "dueDate": "2025-01-15T15:00:00"
    },
    "customer": {
      "name": "Test Customer",
      "email": "test@example.com",
      "phone": "+244900000000"
    },
    "createdDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "updatedDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
  }' | jq .
echo ""

# Test 3: Pending Payment
echo -e "${YELLOW}Test 3: Pending Payment${NC}"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "tx-pending-'$(date +%s)'",
    "merchantTransactionId": "ORDER-PENDING-'$(date +%s)'",
    "type": "Charge",
    "amount": 2000.00,
    "currency": "AOA",
    "status": "Pending",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "987654321",
      "entity": "00123",
      "dueDate": "2025-01-20T23:59:59"
    },
    "customer": {
      "name": "Pending Customer",
      "email": "pending@example.com",
      "phone": "+244900000001"
    },
    "createdDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "updatedDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
  }' | jq .
echo ""

# Test 4: Failed Payment
echo -e "${YELLOW}Test 4: Failed Payment${NC}"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "tx-failed-'$(date +%s)'",
    "merchantTransactionId": "ORDER-FAILED-'$(date +%s)'",
    "type": "Charge",
    "amount": 3000.00,
    "currency": "AOA",
    "status": "Failed",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "111222333",
      "entity": "00123",
      "dueDate": "2025-01-18T23:59:59"
    },
    "customer": {
      "name": "Failed Customer",
      "email": "failed@example.com",
      "phone": "+244900000002"
    },
    "responseStatus": {
      "code": "400",
      "message": "Payment Failed",
      "success": false,
      "errorCode": "INSUFFICIENT_FUNDS",
      "errorDetails": "Insufficient funds in account"
    },
    "createdDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "updatedDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
  }' | jq .
echo ""

# Test 5: Idempotency (send same webhook twice)
echo -e "${YELLOW}Test 5: Idempotency Check${NC}"
IDEMPOTENT_ID="tx-idempotent-$(date +%s)"
IDEMPOTENT_ORDER="ORDER-IDEMPOTENT-$(date +%s)"

echo "First request (should succeed):"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "'$IDEMPOTENT_ID'",
    "merchantTransactionId": "'$IDEMPOTENT_ORDER'",
    "type": "Charge",
    "amount": 1800.00,
    "currency": "AOA",
    "status": "Success",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "444555666",
      "entity": "00123",
      "dueDate": "2025-01-19T23:59:59"
    },
    "customer": {
      "name": "Idempotent Test",
      "email": "idempotent@example.com",
      "phone": "+244900000003"
    },
    "createdDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "updatedDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
  }' | jq .

echo ""
echo "Waiting 3 seconds for processing..."
sleep 3

echo "Second request (should be idempotent):"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "'$IDEMPOTENT_ID'",
    "merchantTransactionId": "'$IDEMPOTENT_ORDER'",
    "type": "Charge",
    "amount": 1800.00,
    "currency": "AOA",
    "status": "Success",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "444555666",
      "entity": "00123",
      "dueDate": "2025-01-19T23:59:59"
    },
    "customer": {
      "name": "Idempotent Test",
      "email": "idempotent@example.com",
      "phone": "+244900000003"
    },
    "createdDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "updatedDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
  }' | jq .
echo ""

# Test 6: Invalid Payload (missing required field)
echo -e "${YELLOW}Test 6: Invalid Payload (should fail)${NC}"
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTransactionId": "ORDER-INVALID",
    "amount": 1000.00,
    "currency": "AOA"
  }' | jq .
echo ""

# Summary
echo "========================================"
echo -e "${GREEN}Test Script Complete${NC}"
echo "========================================"
echo ""
echo "Check application logs for processing details:"
echo "  tail -f logs/application.log"
echo ""
echo "Query database to verify:"
echo "  SELECT * FROM webhook_events ORDER BY created_date DESC LIMIT 5;"
echo "  SELECT * FROM orders ORDER BY created_date DESC LIMIT 5;"
echo "  SELECT * FROM payment_transactions ORDER BY created_date DESC LIMIT 5;"
echo ""
