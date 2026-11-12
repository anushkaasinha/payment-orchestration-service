# Payment Orchestration Service (Spring Boot)

Backend-heavy payment orchestration modelled on production fintech patterns: multi-gateway failover, idempotency,
retry orchestration, signed webhooks with a dead-letter queue, and a double-entry ledger.

## What is implemented
| Capability | How |
|---|---|
| Multi-gateway routing | `GatewayRouter` tries PAYU, PAYTM, STRIPE in priority order. Outage/timeout fails over; hard decline stops (another gateway cannot fix it). |
| Circuit breaker | 3 consecutive transient failures open a gateway for 30s; it is skipped without being called. |
| Idempotency | Unique `(merchant, idempotencyKey)`. Replay returns the original payment (`200` + `Idempotent-Replayed: true`). Same key with a different payload is `409`. Concurrent inserts are resolved by catching the unique-constraint race. |
| Retry orchestration | All gateways failed -> `RETRY_SCHEDULED` with exponential backoff + jitter, a scheduler re-processes due payments, `EXHAUSTED` after `maxRetries`. `@Version` optimistic locking prevents two workers double-processing. |
| Webhooks | HMAC-SHA256 signed, exponential backoff, `DEAD_LETTER` after 5 attempts, admin replay, SSRF guard for public deployments. |
| Ledger | Successful capture writes DEBIT `CUSTOMER_FUNDS` = CREDIT `MERCHANT_PAYABLE` + CREDIT `PLATFORM_REVENUE` (2% fee). |
| Reconciliation | Per-merchant: captured vs ledger debits/credits, fees, net settlement, successes missing ledger entries. |
| Auth | `X-ADMIN-KEY` (admin), `X-API-KEY` (merchant). JSON 401/403. |
| Errors | `GlobalExceptionHandler` returns `{code, message, timestamp}` everywhere. |

Gateways are simulated (`SimulatedGateway`). To go real, implement `PaymentGateway` for each provider SDK.
Admin can flip a gateway to `UP | FLAKY | DOWN | DECLINE` at runtime to demo failover.

## API
Admin (`X-ADMIN-KEY`): `POST /api/v1/admin/merchants`, `GET /api/v1/admin/reconciliation/{merchantId}`,
`GET /api/v1/admin/webhooks/dead-letter`, `POST /api/v1/admin/webhooks/{id}/replay`,
`GET /api/v1/admin/gateways`, `PUT /api/v1/admin/gateways/{PAYU|PAYTM|STRIPE}` body `{"mode":"DOWN"}`

Merchant (`X-API-KEY`): `POST /api/v1/merchant/payments`, `GET /api/v1/merchant/payments`,
`GET /api/v1/merchant/payments/{id}`, `POST /api/v1/merchant/payments/{id}/force-retry`

Demo receiver: `POST /webhook-sink` (always 200).

## Run
```
mvn spring-boot:run      # UI at http://localhost:8080/app/
mvn test
```
Defaults: admin key `admin_secret_123`, seed merchant key `m_demo_merchant_key` (override with `ADMIN_KEY`, `SEED_MERCHANT_KEY`).
H2 is in-memory, so data resets on restart; the seed merchant is recreated at boot.

See `DEPLOY.md` for Render + Vercel deployment.
