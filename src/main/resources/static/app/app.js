const output = document.getElementById("output");

function show(title, data) {
  output.textContent = `${title}\n\n${typeof data === "string" ? data : JSON.stringify(data, null, 2)}`;
}

function showError(title, err, status) {
  const body = err && typeof err === "object" ? JSON.stringify(err, null, 2) : String(err);
  output.textContent = `${title} (status: ${status ?? "unknown"})\n\n${body}`;
}

async function callApi(url, options = {}) {
  const res = await fetch(url, options);
  const text = await res.text();
  let parsed = text;
  try {
    parsed = text ? JSON.parse(text) : {};
  } catch (_) {
  }
  if (!res.ok) {
    throw { status: res.status, body: parsed };
  }
  return parsed;
}

document.getElementById("clearBtn").addEventListener("click", () => {
  output.textContent = "Run any action to see API response.";
});

document.getElementById("createPaymentForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const body = {
    orderId: fd.get("orderId"),
    amount: Number(fd.get("amount")),
    currency: fd.get("currency"),
    idempotencyKey: fd.get("idempotencyKey"),
    webhookUrl: fd.get("webhookUrl"),
    maxRetries: Number(fd.get("maxRetries"))
  };
  const key = fd.get("apiKey");
  try {
    const data = await callApi("/api/v1/merchant/payments", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-API-KEY": key
      },
      body: JSON.stringify(body)
    });
    show("Create Payment: OK", data);
  } catch (e2) {
    showError("Create Payment: FAILED", e2.body ?? e2, e2.status);
  }
});

document.getElementById("getPaymentForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const id = fd.get("paymentId");
  const key = fd.get("apiKey");
  try {
    const data = await callApi(`/api/v1/merchant/payments/${id}`, {
      headers: { "X-API-KEY": key }
    });
    show("Get Payment: OK", data);
  } catch (e2) {
    showError("Get Payment: FAILED", e2.body ?? e2, e2.status);
  }
});

document.getElementById("forceRetryForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const id = fd.get("paymentId");
  const key = fd.get("apiKey");
  try {
    const data = await callApi(`/api/v1/merchant/payments/${id}/force-retry`, {
      method: "POST",
      headers: { "X-API-KEY": key }
    });
    show("Force Retry: OK", data);
  } catch (e2) {
    showError("Force Retry: FAILED", e2.body ?? e2, e2.status);
  }
});

document.getElementById("reconcileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const merchantId = fd.get("merchantId");
  const adminKey = fd.get("adminKey");
  try {
    const data = await callApi(`/api/v1/admin/reconciliation/${merchantId}`, {
      headers: { "X-ADMIN-KEY": adminKey }
    });
    show("Reconciliation: OK", data);
  } catch (e2) {
    showError("Reconciliation: FAILED", e2.body ?? e2, e2.status);
  }
});

document.getElementById("deadLetterForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const adminKey = fd.get("adminKey");
  try {
    const data = await callApi("/api/v1/admin/webhooks/dead-letter", {
      headers: { "X-ADMIN-KEY": adminKey }
    });
    show("Dead-letter Webhooks: OK", data);
  } catch (e2) {
    showError("Dead-letter Webhooks: FAILED", e2.body ?? e2, e2.status);
  }
});
