# Mock AppyPay - Quick Start Prompt

## TL;DR - Copy This to Cursor AI

```
Create a mock AppyPay payment gateway application with:

1. **Web UI** (port 3000):
   - Form to create payments (merchant ID, amount, customer info)
   - Table showing all payments with status
   - Buttons to mark payments as Success/Failed/Cancelled
   - Real-time webhook log viewer

2. **Webhook Sender**:
   - POST to http://localhost:8080/webhooks/appypay
   - Use this exact JSON structure:
   {
     "id": "uuid",
     "merchantTransactionId": "ORDER-xxx",
     "type": "Charge",
     "amount": 1500.00,
     "currency": "AOA",
     "status": "Success|Pending|Failed|Cancelled",
     "paymentMethod": "REF",
     "reference": {
       "referenceNumber": "9-digit-number",
       "entity": "00123",
       "dueDate": "ISO-8601-date"
     },
     "customer": {
       "name": "string",
       "email": "string",
       "phone": "+244..."
     },
     "createdDate": "ISO-8601-timestamp",
     "updatedDate": "ISO-8601-timestamp"
   }
   - Retry 3 times on failure with 2-second delay

3. **Test Scenarios**:
   - "Quick Success Test" button - creates payment + sends success webhook
   - "Failed Payment Test" button - creates payment + sends failed webhook
   - "Batch Test" button - creates 5 payments with mixed statuses

4. **Tech Stack**: Express.js/Flask/Spring Boot (choose one)

5. **Storage**: In-memory (array or SQLite)

Make it simple, clean UI, easy to run alongside port 8080 backend.
Full spec: See MOCK_APPYPAY_PROMPT.md
```

## One-Line Prompts for Different Tech Stacks

### Node.js/Express
```
Create a mock AppyPay payment gateway using Express.js with HTML/CSS/JS frontend, 
in-memory storage, that creates payments and sends webhooks to localhost:8080/webhooks/appypay 
with the JSON structure from AppyPayWebhookPayload.java. Include UI to create payments, 
change status (Success/Failed/Cancelled), and view webhook logs. See MOCK_APPYPAY_PROMPT.md 
for full specification.
```

### Python/Flask
```
Create a mock AppyPay payment gateway using Flask with Jinja2 templates, 
in-memory storage, that creates payments and sends webhooks to localhost:8080/webhooks/appypay 
with the JSON structure from AppyPayWebhookPayload.java. Include web UI to create payments, 
change status (Success/Failed/Cancelled), and view webhook logs. See MOCK_APPYPAY_PROMPT.md 
for full specification.
```

### Java/Spring Boot
```
Create a mock AppyPay payment gateway using Spring Boot with Thymeleaf, 
H2 in-memory database, that creates payments and sends webhooks to localhost:8080/webhooks/appypay 
with the JSON structure from AppyPayWebhookPayload.java. Include web UI to create payments, 
change status (Success/Failed/Cancelled), and view webhook logs. See MOCK_APPYPAY_PROMPT.md 
for full specification.
```

## Key Files to Reference

When Cursor AI asks for more context, provide these files:

1. **Webhook Payload Structure**:
   - `src/main/java/ao/co/oportunidade/webhook/dto/AppyPayWebhookPayload.java`
   - `src/main/java/ao/co/oportunidade/webhook/dto/ReferenceInfo.java`
   - `src/main/java/ao/co/oportunidade/webhook/dto/CustomerInfo.java`

2. **Backend Endpoint**:
   - `src/main/java/ao/co/oportunidade/webhook/resource/AppyPayWebhookResource.java`

3. **Integration Docs**:
   - `docs/APPYPAY_WEBHOOK_INTEGRATION.md`

## Expected Folder Structure

```
mock-appypay/
├── README.md
├── package.json (or requirements.txt)
├── .env.example
├── src/
│   ├── app.js (main server file)
│   ├── paymentService.js
│   └── webhookService.js
└── public/
    ├── index.html
    ├── styles.css
    └── app.js (client-side)
```

## Testing Checklist

After Cursor AI creates the mock:

- [ ] Run: `npm install && npm start` (or equivalent)
- [ ] Open: http://localhost:3000
- [ ] Create a test payment
- [ ] Mark it as "Success"
- [ ] Check backend logs show webhook received
- [ ] Verify payment processed in backend
- [ ] Test "Failed" status
- [ ] Test webhook retry on backend failure
- [ ] Test "Quick Success Test" scenario button
- [ ] Export webhook logs as JSON

## Troubleshooting

**If Cursor AI is confused:**
1. Show it the full `MOCK_APPYPAY_PROMPT.md` file
2. Reference the actual `AppyPayWebhookPayload.java` for exact JSON structure
3. Show example webhook from integration docs
4. Ask it to start with just webhook sending, then add UI

**If webhook fails:**
- Check backend is running on port 8080
- Check `.env` has correct `BACKEND_WEBHOOK_URL`
- Check payload matches `AppyPayWebhookPayload` exactly
- Check `Content-Type: application/json` header is set

## Example .env File

```env
BACKEND_WEBHOOK_URL=http://localhost:8080/webhooks/appypay
PORT=3000
DEFAULT_CURRENCY=AOA
WEBHOOK_RETRY_COUNT=3
WEBHOOK_RETRY_DELAY_MS=2000
LOG_LEVEL=debug
```

## Minimal Viable Product (MVP)

If you need it FAST, ask Cursor AI for just:
1. One HTML page with form
2. Submit creates payment
3. Table shows payments
4. Click button sends webhook
5. Console logs show what happened

Can build from there!

---

**Ready to use?** Copy the TL;DR section above into Cursor AI chat and go! 🚀
