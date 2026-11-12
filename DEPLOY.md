# Deploy: Spring Boot on Render, UI on Vercel

Vercel cannot run a long-lived Java server, so: backend (Docker) on Render, static UI on Vercel proxying to it.
The Render URL alone is already a complete demo (it serves the UI at /app/). Vercel is an optional front door.

## 1. Push to GitHub
```
git init && git add . && git commit -m "payment orchestration"
git branch -M main && git remote add origin https://github.com/<you>/payment-orchestration.git && git push -u origin main
```

## 2. Backend on Render
1. render.com -> New -> Blueprint (or Web Service, runtime Docker) -> pick the repo. `render.yaml` is picked up.
2. Wait for the build (about 5 min). Open Environment, copy the generated `ADMIN_KEY`.
3. Your URL: `https://<name>.onrender.com`. Open `/app/`, paste the admin key in the header.

Free tier sleeps after 15 min idle and takes up to a minute to wake. Open it 5 minutes before the interview.
H2 is in-memory, so data resets on each wake; the seed merchant (`m_demo_merchant_key`) is recreated.

## 3. UI on Vercel (optional)
1. Edit `vercel-frontend/vercel.json`: replace all `YOUR-APP.onrender.com` with your Render host.
2. `cd vercel-frontend && npx vercel --prod` (or import the repo in Vercel with Root Directory = `vercel-frontend`, Framework = Other).
3. Open the Vercel URL. `/api/*` is proxied to Render, so there is no CORS setup.

## Demo script (5 minutes)
1. Create a payment: SUCCESS via PAYU, ledger written.
2. Click Create again with the same key: replayed, no second charge.
3. Set PAYU to DOWN, create a payment: attempts show PAYU fail, then PAYTM success (failover).
4. Set all three to DOWN: RETRY_SCHEDULED, watch backoff and automatic retries; with max retries 0 it goes EXHAUSTED.
5. Set PAYU to DECLINE: FAILED immediately, no failover. Set it back to UP and Force retry.
6. Pick "Unreachable URL" webhook: appears in dead-letter after about 30s; Replay it.
7. Run reconciliation: ledger balanced.
