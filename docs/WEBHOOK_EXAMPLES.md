# AppyPay Webhook Payload Examples

This document contains realistic examples of webhook payloads received from AppyPay.

## 1. Successful Payment

```json
{
  "id": "57af18f2-64b7-4464-a503-3baf741c9f0d",
  "merchantTransactionId": "ORDER-12345",
  "type": "Charge",
  "amount": 1500.00,
  "currency": "AOA",
  "status": "Success",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "123456789",
    "entity": "00123",
    "dueDate": "2025-01-15T15:00:00",
    "startDate": "2025-01-10T00:00:00",
    "status": "Active"
  },
  "customer": {
    "name": "João Silva",
    "email": "joao.silva@example.com",
    "phone": "+244923456789",
    "documentNumber": "004123456BA045",
    "documentType": "ID"
  },
  "events": [
    {
      "type": "Created",
      "status": "Pending",
      "message": "Payment reference created",
      "timestamp": "2025-01-10T11:47:13.521Z"
    },
    {
      "type": "StatusChanged",
      "status": "Success",
      "message": "Payment received at MultiCaixa ATM",
      "timestamp": "2025-01-10T11:47:52.599Z"
    }
  ],
  "responseStatus": {
    "code": "200",
    "message": "Success",
    "success": true
  },
  "createdDate": "2025-01-10T11:47:13.521Z",
  "updatedDate": "2025-01-10T11:47:52.599Z",
  "metadata": {
    "atmId": "ATM-001",
    "location": "Luanda, Angola"
  }
}
```

## 2. Pending Payment

```json
{
  "id": "a3b2c1d4-5e6f-7g8h-9i0j-1k2l3m4n5o6p",
  "merchantTransactionId": "ORDER-67890",
  "type": "Charge",
  "amount": 2500.00,
  "currency": "AOA",
  "status": "Pending",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "987654321",
    "entity": "00123",
    "dueDate": "2025-01-20T23:59:59",
    "startDate": "2025-01-10T00:00:00",
    "status": "Active"
  },
  "customer": {
    "name": "Maria Santos",
    "email": "maria.santos@example.com",
    "phone": "+244929876543",
    "documentNumber": "005987654BA046",
    "documentType": "ID"
  },
  "events": [
    {
      "type": "Created",
      "status": "Pending",
      "message": "Payment reference created and awaiting payment",
      "timestamp": "2025-01-10T14:30:00.000Z"
    }
  ],
  "responseStatus": {
    "code": "202",
    "message": "Pending",
    "success": true
  },
  "createdDate": "2025-01-10T14:30:00.000Z",
  "updatedDate": "2025-01-10T14:30:00.000Z"
}
```

## 3. Failed Payment

```json
{
  "id": "f9e8d7c6-b5a4-3210-fedc-ba9876543210",
  "merchantTransactionId": "ORDER-11111",
  "type": "Charge",
  "amount": 3000.00,
  "currency": "AOA",
  "status": "Failed",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "111222333",
    "entity": "00123",
    "dueDate": "2025-01-18T23:59:59",
    "startDate": "2025-01-10T00:00:00",
    "status": "Expired"
  },
  "customer": {
    "name": "Pedro Costa",
    "email": "pedro.costa@example.com",
    "phone": "+244931112222",
    "documentNumber": "006111222BA047",
    "documentType": "ID"
  },
  "events": [
    {
      "type": "Created",
      "status": "Pending",
      "message": "Payment reference created",
      "timestamp": "2025-01-10T09:00:00.000Z"
    },
    {
      "type": "StatusChanged",
      "status": "Failed",
      "message": "Payment failed due to insufficient funds",
      "timestamp": "2025-01-10T16:45:30.123Z"
    }
  ],
  "responseStatus": {
    "code": "400",
    "message": "Payment Failed",
    "success": false,
    "errorCode": "INSUFFICIENT_FUNDS",
    "errorDetails": "Customer account has insufficient funds to complete the transaction"
  },
  "createdDate": "2025-01-10T09:00:00.000Z",
  "updatedDate": "2025-01-10T16:45:30.123Z"
}
```

## 4. Cancelled Payment

```json
{
  "id": "1a2b3c4d-5e6f-7g8h-9i0j-0k1l2m3n4o5p",
  "merchantTransactionId": "ORDER-22222",
  "type": "Charge",
  "amount": 1800.00,
  "currency": "AOA",
  "status": "Cancelled",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "444555666",
    "entity": "00123",
    "dueDate": "2025-01-19T23:59:59",
    "startDate": "2025-01-10T00:00:00",
    "status": "Cancelled"
  },
  "customer": {
    "name": "Ana Ferreira",
    "email": "ana.ferreira@example.com",
    "phone": "+244934445555",
    "documentNumber": "007444555BA048",
    "documentType": "ID"
  },
  "events": [
    {
      "type": "Created",
      "status": "Pending",
      "message": "Payment reference created",
      "timestamp": "2025-01-10T10:15:00.000Z"
    },
    {
      "type": "StatusChanged",
      "status": "Cancelled",
      "message": "Payment cancelled by merchant",
      "timestamp": "2025-01-10T12:30:45.678Z"
    }
  ],
  "responseStatus": {
    "code": "200",
    "message": "Cancelled",
    "success": true
  },
  "createdDate": "2025-01-10T10:15:00.000Z",
  "updatedDate": "2025-01-10T12:30:45.678Z",
  "metadata": {
    "cancellationReason": "Customer request",
    "cancelledBy": "merchant"
  }
}
```

## 5. Refund Notification

```json
{
  "id": "9z8y7x6w-5v4u-3t2s-1r0q-9p8o7n6m5l4k",
  "merchantTransactionId": "ORDER-33333",
  "type": "Refund",
  "amount": 2200.00,
  "currency": "AOA",
  "status": "Success",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "777888999",
    "entity": "00123",
    "dueDate": "2025-01-17T23:59:59",
    "startDate": "2025-01-08T00:00:00",
    "status": "Completed"
  },
  "customer": {
    "name": "Carlos Mendes",
    "email": "carlos.mendes@example.com",
    "phone": "+244937778888",
    "documentNumber": "008777888BA049",
    "documentType": "ID"
  },
  "events": [
    {
      "type": "Created",
      "status": "Pending",
      "message": "Payment reference created",
      "timestamp": "2025-01-08T08:00:00.000Z"
    },
    {
      "type": "StatusChanged",
      "status": "Success",
      "message": "Payment received",
      "timestamp": "2025-01-08T14:20:30.456Z"
    },
    {
      "type": "Refund",
      "status": "Success",
      "message": "Refund processed successfully",
      "timestamp": "2025-01-10T11:00:00.789Z"
    }
  ],
  "responseStatus": {
    "code": "200",
    "message": "Refund Successful",
    "success": true
  },
  "createdDate": "2025-01-08T08:00:00.000Z",
  "updatedDate": "2025-01-10T11:00:00.789Z",
  "metadata": {
    "originalTransactionId": "original-tx-12345",
    "refundReason": "Product return",
    "refundedBy": "merchant"
  }
}
```

## Testing with cURL

### Test Success Webhook

```bash
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-tx-123",
    "merchantTransactionId": "ORDER-TEST-001",
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
    "createdDate": "2025-01-10T11:47:13.521Z",
    "updatedDate": "2025-01-10T11:47:52.599Z"
  }'
```

### Test Idempotency (Send same webhook twice)

```bash
# First request
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{"id": "same-tx-id", "merchantTransactionId": "ORDER-001", ...}'

# Second request (should return "already_processed")
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{"id": "same-tx-id", "merchantTransactionId": "ORDER-001", ...}'
```

## Notes

- All timestamps are in ISO 8601 format with UTC timezone
- Amounts are in Angolan Kwanza (AOA)
- Reference numbers are 9-digit codes used at MultiCaixa ATMs
- Entity codes identify the payment recipient
- Customer document types include: ID (Identity Card), Passport, etc.
