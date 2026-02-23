# Cursor AI Prompt: Create Mock AppyPay Payment Gateway

## Objective

Create a standalone mock application that emulates the AppyPay payment gateway system for testing webhook integration with the Oportunidade recruiting backend. This mock should simulate the complete payment flow from payment initiation to webhook notification.

## Context

AppyPay is a payment gateway used in Angola that processes payments via MultiCaixa ATM references. Our Oportunidade backend receives payment notifications via webhooks at `POST /webhooks/appypay`. We need a mock service to:
1. Simulate the AppyPay payment portal
2. Generate payment references
3. Send webhook notifications to our backend
4. Allow testing of different payment scenarios (success, pending, failed, cancelled)

## Technical Requirements

### Technology Stack
- **Framework**: Express.js (Node.js) or Flask (Python) or Spring Boot (Java)
- **Frontend**: Simple HTML/CSS/JavaScript (no framework needed)
- **Storage**: In-memory or SQLite for simplicity
- **Port**: 3000 (configurable)

### Core Features Required

#### 1. Payment Portal UI (Web Interface)
Create a simple web interface at `http://localhost:3000` with:

**Home Page:**
- Form to initiate a payment with fields:
  - Merchant Transaction ID (auto-generated or manual)
  - Amount (number input, default AOA currency)
  - Customer Name
  - Customer Email  
  - Customer Phone (Angola format: +244...)
  - Reference Number (auto-generated 9-digit number)
  - Entity Code (default: "00123")
  - Due Date (date picker)
- "Create Payment" button

**Payment List Page:**
- Table showing all created payments with columns:
  - Transaction ID
  - Merchant Transaction ID
  - Amount
  - Status (Pending, Success, Failed, Cancelled)
  - Created Date
  - Actions (buttons to change status)
- Action buttons for each payment:
  - "Mark as Success" → sends Success webhook
  - "Mark as Failed" → sends Failed webhook
  - "Mark as Cancelled" → sends Cancelled webhook
  - "Keep Pending" → no webhook sent
- Search/filter by transaction ID or status

#### 2. Webhook Sender API

**Endpoint**: Internal (not exposed, triggered by UI actions)

**Webhook Payload Structure** (send to configured backend URL):

```json
{
  "id": "uuid-v4-transaction-id",
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
    "status": "ACTIVE"
  },
  "events": [
    {
      "type": "payment.created",
      "status": "Pending",
      "timestamp": "2025-01-10T11:47:13.521Z"
    },
    {
      "type": "payment.completed",
      "status": "Success",
      "timestamp": "2025-01-10T11:47:52.599Z"
    }
  ],
  "responseStatus": {
    "code": "00",
    "message": "Transaction successful"
  },
  "customer": {
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+244900000000",
    "documentNumber": null,
    "documentType": null
  },
  "createdDate": "2025-01-10T11:47:13.521Z",
  "updatedDate": "2025-01-10T11:47:52.599Z",
  "metadata": null
}
```

**Webhook Behavior:**
- POST to `http://localhost:8080/webhooks/appypay` (configurable)
- Retry logic: 3 attempts with 2-second delay if webhook fails
- Log all webhook attempts (success/failure) in the UI
- Display webhook response from backend

#### 3. Configuration

Create a configuration file or environment variables:
```env
BACKEND_WEBHOOK_URL=http://localhost:8080/webhooks/appypay
MOCK_APPYPAY_PORT=3000
DEFAULT_CURRENCY=AOA
WEBHOOK_RETRY_COUNT=3
WEBHOOK_RETRY_DELAY_MS=2000
```

#### 4. Test Scenarios

Include preset test scenarios accessible via UI:

**Scenario Buttons:**
1. **"Quick Success Test"**
   - Creates payment with auto-generated data
   - Immediately marks as Success
   - Sends webhook
   - Shows result

2. **"Delayed Payment Simulation"**
   - Creates payment in Pending status
   - Wait 5 seconds (simulated ATM delay)
   - Automatically marks as Success
   - Sends webhook

3. **"Failed Payment Test"**
   - Creates payment
   - Immediately marks as Failed
   - Sends webhook with error message

4. **"Batch Payment Test"**
   - Creates 5 payments at once
   - Randomly assigns statuses (3 success, 1 pending, 1 failed)
   - Sends webhooks for completed ones

#### 5. Logging & Debug Features

**Console Logs:**
- Log every payment created
- Log every webhook sent (with full payload)
- Log webhook responses from backend
- Color-coded logs (green=success, red=error, yellow=pending)

**Debug Panel in UI:**
- Real-time webhook log viewer
- Copy webhook payload button
- Clear logs button
- Export logs as JSON

## Implementation Details

### Payment Status Flow

```
[Payment Created] → Status: Pending
                    ↓
        [User clicks action button]
                    ↓
        ┌───────────┼───────────┐
        ↓           ↓           ↓
    Success      Failed    Cancelled
        ↓           ↓           ↓
   [Send Webhook with respective status]
        ↓
   [Log response]
```

### Data Model

**Payment Object:**
```javascript
{
  id: "uuid",
  merchantTransactionId: "ORDER-123",
  amount: 1500.00,
  currency: "AOA",
  status: "Pending|Success|Failed|Cancelled",
  paymentMethod: "REF",
  customer: {
    name: "John Doe",
    email: "john@example.com",
    phone: "+244900000000"
  },
  reference: {
    referenceNumber: "123456789",
    entity: "00123",
    dueDate: "2025-01-15T15:00:00"
  },
  createdDate: "2025-01-10T11:47:13.521Z",
  updatedDate: "2025-01-10T11:47:52.599Z",
  webhookAttempts: [
    {
      timestamp: "2025-01-10T11:47:52.599Z",
      status: "SUCCESS|FAILED",
      responseCode: 200,
      responseBody: "...",
      error: null
    }
  ]
}
```

## User Interface Design

### Layout
```
┌─────────────────────────────────────────────────────────┐
│                  Mock AppyPay Gateway                    │
├─────────────────────────────────────────────────────────┤
│  [Create Payment] [View Payments] [Test Scenarios] [Config] │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Create New Payment                                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Merchant Tx ID: [AUTO-ORD-xxx] [⚙ Custom]       │  │
│  │ Amount (AOA):   [1500.00]                        │  │
│  │ Customer Name:  [John Doe]                       │  │
│  │ Customer Email: [john@test.com]                  │  │
│  │ Customer Phone: [+244900000000]                  │  │
│  │ Reference #:    [123456789] (9 digits)           │  │
│  │ Entity Code:    [00123]                          │  │
│  │ Due Date:       [2025-12-31] 📅                  │  │
│  │                                                   │  │
│  │         [Create Payment] [Reset Form]            │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Quick Test Scenarios                                    │
│  [✓ Quick Success] [⏱ Delayed Success]                  │
│  [✗ Failed Payment] [📊 Batch Test (5 payments)]        │
│                                                          │
├─────────────────────────────────────────────────────────┤
│  Recent Payments                                         │
│  ┌─────┬─────────┬────────┬────────┬──────────────────┐│
│  │ ID  │ Merch.  │ Amount │ Status │ Actions          ││
│  ├─────┼─────────┼────────┼────────┼──────────────────┤│
│  │ abc │ ORD-123 │ 1500   │🟡Pending│[✓][✗][⊗][🔄]   ││
│  │ def │ ORD-124 │ 2000   │✅Success│ Webhook sent ✓  ││
│  │ ghi │ ORD-125 │ 1000   │❌Failed │ Webhook sent ✓  ││
│  └─────┴─────────┴────────┴────────┴──────────────────┘│
│                                                          │
│  Webhook Log (Last 10)                                   │
│  ┌───────────────────────────────────────────────────┐ │
│  │ 14:23:45 → SUCCESS (200 OK) - Transaction abc    │ │
│  │ 14:20:12 → FAILED (500) - Retry 1/3              │ │
│  │ 14:20:14 → SUCCESS (200 OK) - Retry succeeded    │ │
│  │ [Clear Log] [Export JSON] [Copy Last Payload]    │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### CSS Styling
- Modern, clean design
- Use status colors: Green (Success), Yellow (Pending), Red (Failed), Gray (Cancelled)
- Responsive layout (works on mobile)
- Monospace font for transaction IDs and JSON
- Highlight recent activity

## File Structure

```
mock-appypay/
├── package.json (or requirements.txt / pom.xml)
├── .env.example
├── README.md
├── src/
│   ├── app.js (or main.py / Application.java)
│   ├── routes/
│   │   ├── payments.js
│   │   └── webhooks.js
│   ├── services/
│   │   ├── paymentService.js
│   │   └── webhookService.js
│   ├── storage/
│   │   └── inMemoryStore.js
│   └── utils/
│       ├── generator.js (UUID, reference numbers)
│       └── logger.js
├── public/
│   ├── index.html
│   ├── styles.css
│   └── app.js (client-side JavaScript)
└── tests/
    ├── payment.test.js
    └── webhook.test.js
```

## API Endpoints (for programmatic access)

```
POST   /api/payments              - Create new payment
GET    /api/payments              - List all payments
GET    /api/payments/:id          - Get payment details
PUT    /api/payments/:id/status   - Update payment status
POST   /api/payments/:id/webhook  - Trigger webhook manually
DELETE /api/payments/:id          - Delete payment
GET    /api/webhook-logs          - Get webhook attempt logs
POST   /api/test-scenarios/:name  - Run test scenario
```

## Success Criteria

The mock AppyPay application is successful when:

1. ✅ Can create payments with all required fields
2. ✅ Can change payment status (Pending → Success/Failed/Cancelled)
3. ✅ Sends correct webhook payload to configured backend URL
4. ✅ Implements retry logic (3 attempts)
5. ✅ Displays all payments in a table with filter/search
6. ✅ Shows webhook logs with request/response details
7. ✅ Test scenarios work correctly
8. ✅ UI is clean, responsive, and easy to use
9. ✅ Works standalone without external dependencies
10. ✅ Includes README with setup instructions

## Testing Instructions

After creation, test with:

```bash
# Terminal 1: Start Oportunidade backend
cd recruiting
./mvnw quarkus:dev

# Terminal 2: Start mock AppyPay
cd mock-appypay
npm start  # or python app.py or ./mvnw quarkus:dev

# Open browser
http://localhost:3000

# Run quick test
1. Click "Quick Success Test" button
2. Verify webhook appears in backend logs
3. Check payment appears as SUCCESS in backend database
```

## Additional Features (Nice to Have)

- **Webhook signature**: Add HMAC signature to webhooks for security testing
- **Delay simulator**: Add configurable delay before sending webhooks
- **Webhook inspector**: Built-in request/response inspector
- **Export/Import**: Export payments as JSON, import test data
- **Statistics dashboard**: Show success rate, average amount, etc.
- **Dark mode**: Toggle dark/light theme
- **API documentation**: Swagger/OpenAPI docs at `/api-docs`

## Example Usage Scenarios

### Scenario 1: Test Successful Payment Flow
```
1. Open mock AppyPay UI
2. Fill in payment form with test data
3. Click "Create Payment"
4. Payment appears in list with "Pending" status
5. Click "Mark as Success" button
6. Webhook sent to backend (200 OK response shown)
7. Backend processes payment and creates order
8. Verify in backend: Order created, token generated, email sent
```

### Scenario 2: Test Failed Payment
```
1. Create payment via UI
2. Click "Mark as Failed"
3. Webhook sent with Failed status
4. Backend receives webhook, marks order as failed
5. Verify no token generated, no email sent
```

### Scenario 3: Test Retry Logic
```
1. Stop backend temporarily
2. Create payment and mark as Success
3. Webhook attempt 1 fails → Mock shows "Retry 1/3"
4. Webhook attempt 2 fails → Mock shows "Retry 2/3"
5. Start backend
6. Webhook attempt 3 succeeds → Mock shows "SUCCESS (200 OK)"
```

## Deliverables

Please create:
1. Complete source code for mock AppyPay application
2. README.md with setup and usage instructions
3. .env.example with configuration template
4. package.json/requirements.txt with all dependencies
5. Sample test data (5 example payments)

## Notes

- Keep it simple - this is a mock for testing, not production
- Focus on functionality over perfect UI
- Ensure webhook payload exactly matches AppyPay specification
- Make it easy to run alongside the main application
- Include clear logging for debugging
- Make configuration easy (single .env file)

---

**Start by confirming you understand the requirements, then proceed with implementation. Begin with the core webhook sending functionality and basic UI, then add advanced features.**
