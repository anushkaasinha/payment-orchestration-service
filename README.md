# Payment Orchestration Service (Spring Boot)

A backend-heavy, resume-grade project that models payment orchestration used in fintech and large-scale commerce systems.

This project is designed to showcase the kind of backend engineering expected in companies like Razorpay, Amazon, and Google:
- idempotent payment APIs
- retry orchestration with exponential backoff
- webhook delivery engine with dead-letter queue behavior
- ledger creation for reconciliation
- API-key based auth (merchant + admin)
- validation, global exception handling, and automated tests

## Tech Stack
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA (Hibernate)
- H2 (in-memory DB)
- Maven
- JUnit 5 + MockMvc

## High-Level Architecture
- `Merchant APIs` create/query/retry payments
- `GatewaySimulator` simulates success / transient / hard failures
- `Payment Retry Scheduler` reprocesses due payments
- `Webhook Dispatcher` retries outbound webhook events and pushes irrecoverable events to dead-letter status
- `Ledger Entries` (DEBIT/CREDIT) are generated for successful captures
- `Admin APIs` onboard merchants, run reconciliation, inspect dead-letter webhooks

## Project Structure
- `/src/main/java/com/resumebackend/payments/controller` -> REST APIs
- `/src/main/java/com/resumebackend/payments/service` -> business logic + schedulers
- `/src/main/java/com/resumebackend/payments/domain` -> entities and enums
- `/src/main/java/com/resumebackend/payments/repository` -> JPA repositories
- `/src/main/java/com/resumebackend/payments/security` -> API key auth filter + principal
- `/src/main/java/com/resumebackend/payments/exception` -> centralized exception handling
- `/src/test/java/com/resumebackend/payments` -> tests

## Security Model
- Admin endpoints: header `X-ADMIN-KEY`
- Merchant endpoints: header `X-API-KEY`

Default values (`application.yml`):
- Admin key: `admin_secret_123`
- Seed merchant API key: `m_demo_merchant_key`

## API Endpoints

### Admin
- `POST /api/v1/admin/merchants` -> create merchant
- `GET /api/v1/admin/reconciliation/{merchantId}` -> summary settlement view
- `GET /api/v1/admin/webhooks/dead-letter` -> inspect failed webhooks

### Merchant
- `POST /api/v1/merchant/payments` -> create + process payment intent
- `GET /api/v1/merchant/payments/{paymentId}` -> payment details + attempt history
- `POST /api/v1/merchant/payments/{paymentId}/force-retry` -> manual retry

## Example Flow

### 1. Create merchant (admin)
```bash
curl -X POST http://localhost:8080/api/v1/admin/merchants \
  -H "X-ADMIN-KEY: admin_secret_123" \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp"}'
```

### 2. Create payment (merchant)
```bash
curl -X POST http://localhost:8080/api/v1/merchant/payments \
  -H "X-API-KEY: m_demo_merchant_key" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-2026-001",
    "amount": 1299.00,
    "currency": "INR",
    "idempotencyKey": "idem-2026-001",
    "webhookUrl": "https://merchant.example/webhook/payment",
    "maxRetries": 3
  }'
```

### 3. Query payment
```bash
curl http://localhost:8080/api/v1/merchant/payments/1 \
  -H "X-API-KEY: m_demo_merchant_key"
```

### 4. Force retry (if failed/exhausted)
```bash
curl -X POST http://localhost:8080/api/v1/merchant/payments/1/force-retry \
  -H "X-API-KEY: m_demo_merchant_key"
```

## Local Run
```bash
mvn spring-boot:run
```

## Web UI (Frontend Attached)
A lightweight frontend is bundled into Spring Boot static assets.

- Open: `http://localhost:8080/app/` (or your active port, e.g. `8082`)
- No separate React build is required; it is served directly by this backend.
- The UI lets you:
- create payment
- fetch payment
- force retry
- run reconciliation
- inspect dead-letter webhooks

## Run Tests
```bash
mvn test
```

## Why This Project Is Strong For Resume
- Demonstrates core payment backend patterns, not CRUD-only APIs.
- Shows reliability engineering (idempotency, retries, backoff, DLQ-style handling).
- Includes accounting-adjacent concepts (ledger + reconciliation).
- Uses production-style layering, validation, security, and tests.

## Suggested Resume Bullet (you can use directly)
Built a Spring Boot payment orchestration service with idempotent APIs, automated retry workflows, webhook dispatch with dead-letter handling, and reconciliation ledger generation; improved simulated payment reliability and demonstrated production-grade backend design patterns.
