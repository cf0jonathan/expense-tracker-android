// filesForServer/server/index.js
// Minimal Plaid demo server for Replit (or local testing).
// IMPORTANT: For Replit, set the environment variables in the Replit Secrets UI:
//   PLAID_CLIENT_ID, PLAID_SECRET, PLAID_ENV (defaults to "sandbox"), DEMO_API_KEY
// If you run locally, you can set these in your shell or use a .env loader.

const express = require('express');
const axios = require('axios');
const cors = require('cors');

// Load local .env file when present (for local testing only).
// IMPORTANT: Do NOT commit a real .env file to git. Use the host's secret manager for deployments.
try {
  require('dotenv').config();
} catch (e) {
  // dotenv not installed or .env not present — that's fine for deployed environments.
}

const app = express();
app.use(express.json());
app.use(cors());

// ========= CONFIG (replace with env vars on the host) =========
// For security do NOT hardcode real secrets in source. Instead set them using
// Replit secrets or your host's env var mechanism.
const PLAID_CLIENT_ID = process.env.PLAID_CLIENT_ID || '<REPLACE_WITH_PLAID_CLIENT_ID>'; // <-- replace or set env
const PLAID_SECRET = process.env.PLAID_SECRET || '<REPLACE_WITH_PLAID_SECRET>'; // <-- replace or set env
const PLAID_ENV = process.env.PLAID_ENV || 'sandbox'; // sandbox | development | production
const DEMO_API_KEY = process.env.DEMO_API_KEY || '<REPLACE_WITH_DEMO_API_KEY>'; // <-- replace or set env

// Plaid base URL selection
const PLAID_BASE = {
  sandbox: 'https://sandbox.plaid.com',
  development: 'https://development.plaid.com',
  production: 'https://production.plaid.com'
}[PLAID_ENV];

// FEATURE: FAKE_PLAID enables returning hardcoded Plaid-like transactions for sandbox/dev testing.
// Default: enabled when PLAID_ENV === 'sandbox' or when env FAKE_PLAID=true
const FAKE_PLAID = false;

// Simple helper that returns a Plaid-like /transactions/get response object for demo/testing.
function generateFakeTransactions(accessToken) {
  const today = new Date();
  const fmt = (d) => d.toISOString().slice(0, 10);
  const accounts = [
    { account_id: 'acct_plaid_1', balances: {}, holder_category: 'personal', mask: '0000', name: 'Plaid Checking', official_name: 'Plaid Gold Standard Checking', subtype: 'checking', type: 'depository' }
  ];

  const transactions = [
    {
      account_id: 'acct_plaid_1',
      amount: 4.5,
      date: fmt(new Date(today.getTime() - 1000 * 60 * 60 * 24 * 2)),
      name: 'Demo Coffee',
      merchant_name: 'Demo Coffee',
      transaction_id: `tx_${Math.random().toString(36).substring(2,10)}`,
      iso_currency_code: 'USD'
    },
    {
      account_id: 'acct_plaid_1',
      amount: 32.75,
      date: fmt(new Date(today.getTime() - 1000 * 60 * 60 * 24 * 5)),
      name: 'Demo Groceries',
      merchant_name: 'Demo Market',
      transaction_id: `tx_${Math.random().toString(36).substring(2,10)}`,
      iso_currency_code: 'USD'
    },
    {
      account_id: 'acct_plaid_1',
      amount: -1500.00,
      date: fmt(new Date(today.getTime() - 1000 * 60 * 60 * 24 * 20)),
      name: 'Demo Salary',
      merchant_name: 'Employer Inc',
      transaction_id: `tx_${Math.random().toString(36).substring(2,10)}`,
      iso_currency_code: 'USD'
    }
  ];

  return {
    accounts,
    item: { item_id: `item_${accessToken?.slice?.(0,8) || 'demo'}` },
    request_id: `req_${Math.random().toString(36).substring(2,8)}`,
    total_transactions: transactions.length,
    transactions
  };
}

if (!PLAID_CLIENT_ID || PLAID_CLIENT_ID.startsWith('<REPLACE')) {
  console.warn('Warning: PLAID_CLIENT_ID not set. Set environment variable PLAID_CLIENT_ID.');
}
if (!PLAID_SECRET || PLAID_SECRET.startsWith('<REPLACE')) {
  console.warn('Warning: PLAID_SECRET not set. Set environment variable PLAID_SECRET.');
}
if (!DEMO_API_KEY || DEMO_API_KEY.startsWith('<REPLACE')) {
  console.warn('Warning: DEMO_API_KEY not set. Set environment variable DEMO_API_KEY.');
}

// Simple middleware to require a demo API key on requests.
// On Android, set header: x-demo-key: <DEMO_API_KEY>
function requireDemoKey(req, res, next) {
  const key = req.header('x-demo-key') || '';
  if (!DEMO_API_KEY || key !== DEMO_API_KEY) {
    return res.status(401).json({ error: 'unauthorized - missing or invalid demo key' });
  }
  next();
}

// In-memory demo store for access_tokens and transactions (for demo purposes only)
const ITEM_ACCESS_TOKENS = {}; // item_id -> access_token
const ITEM_TRANSACTIONS = {}; // access_token -> transactions response

// Root - helpful info
app.get('/', (req, res) => {
  res.send({
    message: 'Plaid demo server (filesForServer). Use /create_link_token and /exchange_public_token',
    note: 'Set x-demo-key header to the DEMO_API_KEY value.'
  });
});

// Create link token - mobile app calls this to get a link_token for Plaid Link
app.get('/create_link_token', requireDemoKey, async (req, res) => {
  try {
    const userId = req.query.client_user_id || `student-${Date.now()}`; // optional: client may pass the user id
    const body = {
      client_id: PLAID_CLIENT_ID,
      secret: PLAID_SECRET,
      client_name: 'Expense Tracker Demo',
      products: ['transactions', 'auth'],
      country_codes: ['US'],
      language: 'en',
      user: { client_user_id: userId }
    };

    const resp = await axios.post(`${PLAID_BASE}/link/token/create`, body, { timeout: 10000 });
    // resp.data contains link_token and metadata. Return directly to client.
    res.json(resp.data);
  } catch (err) {
    console.error('link token create error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'link token creation failed', details: err?.response?.data || err?.message });
  }
});

// Create sandbox public_token helper (placed before exchange_public_token)
// When FAKE_PLAID is enabled this returns a synthetic public_token so the client can proceed in dev.
app.post('/create_sandbox_public_token', requireDemoKey, async (req, res) => {
  try {
    const initial_products = req.body?.initial_products || ['transactions'];
    const institution_id = req.body?.institution_id || 'ins_109508';

    if (FAKE_PLAID) {
      const public_token = `public-fake-${Math.random().toString(36).substring(2,10)}`;
      const request_id = `req-${Math.random().toString(36).substring(2,8)}`;
      console.log('FAKE_PLAID: created fake public_token', public_token, 'for products', initial_products);
      return res.json({ public_token, request_id });
    }

    if (PLAID_ENV !== 'sandbox') {
      return res.status(400).json({ error: 'create_sandbox_public_token is only available in sandbox mode' });
    }

    const body = {
      client_id: PLAID_CLIENT_ID,
      secret: PLAID_SECRET,
      institution_id,
      initial_products
    };

    const resp = await axios.post(`${PLAID_BASE}/sandbox/public_token/create`, body, { timeout: 10000 });
    return res.json(resp.data);
  } catch (err) {
    console.error('create_sandbox_public_token error:', err?.response?.status, err?.response?.data || err?.message || err);
    const status = err?.response?.status || 502;
    const details = err?.response?.data || err?.message;
    return res.status(status).json({ error: 'create_sandbox_public_token failed', details });
  }
});

// Exchange public_token for access_token - mobile app POSTs public_token here
app.post('/exchange_public_token', requireDemoKey, async (req, res) => {
  try {
    const { public_token } = req.body;
    if (!public_token) return res.status(400).json({ error: 'missing public_token in body' });

    // If FAKE_PLAID is enabled, synthesize an access_token and cached transactions for demo/testing.
    if (FAKE_PLAID) {
      const access_token = `access-fake-${Math.random().toString(36).substring(2,10)}`;
      const item_id = `item-fake-${Math.random().toString(36).substring(2,8)}`;
      ITEM_ACCESS_TOKENS[item_id] = access_token;
      // generate and cache fake transactions immediately
      ITEM_TRANSACTIONS[access_token] = generateFakeTransactions(access_token);
      console.log(`FAKE_PLAID enabled: created fake access_token for item_id=${item_id}`);

      const out = { access_token, item_id, request_id: `req-${Math.random().toString(36).substring(2,8)}` };
      out.cached_transactions = ITEM_TRANSACTIONS[access_token];
      out.cached_transactions_present = true;
      return res.json(out);
    }

    const body = {
      client_id: PLAID_CLIENT_ID,
      secret: PLAID_SECRET,
      public_token
    };

    const resp = await axios.post(`${PLAID_BASE}/item/public_token/exchange`, body, { timeout: 10000 });

    // Store the access_token in-memory (demo only). Save by item_id so webhooks can reference it.
    const access_token = resp.data?.access_token;
    const item_id = resp.data?.item_id;
    if (item_id && access_token) {
      ITEM_ACCESS_TOKENS[item_id] = access_token;
      console.log(`Stored access_token for item_id=${item_id}`);
    }

    // In sandbox, proactively fire the INITIAL_UPDATE webhook and try to cache transactions so clients don't race.
    if (PLAID_ENV === 'sandbox' && access_token) {
      (async () => {
        try {
          console.log('Sandbox auto-fire: firing INITIAL_UPDATE for access_token');
          await axios.post(`${PLAID_BASE}/sandbox/item/fire_webhook`, { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token, webhook_code: 'INITIAL_UPDATE' }, { timeout: 10000 });
          // Try to fetch transactions immediately and cache them
          try {
            const end = new Date().toISOString().slice(0, 10);
            const start = new Date(Date.now() - 1000 * 60 * 60 * 24 * 30).toISOString().slice(0, 10);
            const txResp = await axios.post(`${PLAID_BASE}/transactions/get`, { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token, start_date: start, end_date: end }, { timeout: 20000 });
            ITEM_TRANSACTIONS[access_token] = txResp.data;
            console.log('Auto-cached transactions after exchange (sandbox)', txResp.data?.total_transactions || 0);
          } catch (fetchErr) {
            console.warn('Auto-fetch transactions after sandbox webhook failed', fetchErr?.response?.data || fetchErr?.message || fetchErr);
          }
        } catch (fireErr) {
          console.warn('Auto-fire sandbox webhook failed', fireErr?.response?.data || fireErr?.message || fireErr);
        }
      })();
    }

    // FOR DEMO ONLY: returning access_token here so you can inspect it in the client.
    // Additionally, include cached transactions if the server already fetched them (sandbox auto-fetch) so clients can avoid a race.
    // DO NOT return access_token in a real production app. Instead store it securely server-side.
    const out = Object.assign({}, resp.data);
    if (access_token && ITEM_TRANSACTIONS[access_token]) {
      out.cached_transactions = ITEM_TRANSACTIONS[access_token];
      out.cached_transactions_present = true;
    } else {
      out.cached_transactions_present = false;
    }
    res.json(out);
  } catch (err) {
    console.error('public_token exchange error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'public_token exchange failed', details: err?.response?.data || err?.message });
  }
});

// Optional: endpoint to fetch transactions using stored access_token (DEMO only)
// For a real app, you should store the access_token and call Plaid from the server.
app.post('/transactions_for_access_token', requireDemoKey, async (req, res) => {
  try {
    const { access_token, start_date, end_date } = req.body;
    if (!access_token) return res.status(400).json({ error: 'missing access_token in body' });

    // If FAKE_PLAID is enabled, generate or return cached fake transactions immediately.
    if (FAKE_PLAID) {
      if (!ITEM_TRANSACTIONS[access_token]) {
        ITEM_TRANSACTIONS[access_token] = generateFakeTransactions(access_token);
        console.log(`FAKE_PLAID: generated fake transactions for access_token=${access_token}`);
      } else {
        console.log(`FAKE_PLAID: returning cached fake transactions for access_token=${access_token}`);
      }
      return res.json(ITEM_TRANSACTIONS[access_token]);
    }

    // If we have cached transactions for this access_token (populated by webhook or earlier fetch), return them immediately.
    if (ITEM_TRANSACTIONS[access_token]) {
      console.log('Returning cached transactions for access_token');
      return res.json(ITEM_TRANSACTIONS[access_token]);
    }

    // Poll Plaid /transactions/sync with backoff (server-side) to wait until transactions are ready.
    // This avoids returning PRODUCT_NOT_READY to the client.
    const maxAttempts = 10;
    let attempt = 0;
    let lastSyncResp = null;
    const start = start_date || new Date(Date.now() - 1000 * 60 * 60 * 24 * 30).toISOString().slice(0, 10);
    const end = end_date || new Date().toISOString().slice(0, 10);

    while (attempt < maxAttempts) {
      attempt++;
      try {
        console.log(`Polling transactions/sync attempt=${attempt} for access_token`);
        const syncResp = await axios.post(`${PLAID_BASE}/transactions/sync`, { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token }, { timeout: 20000 });
        lastSyncResp = syncResp.data;

        // If sync reports added[] items or update status not NOT_READY, proceed to fetch full transactions.
        const added = syncResp.data?.added;
        const updateStatus = syncResp.data?.transactions_update_status;
        if ((Array.isArray(added) && added.length > 0) || (typeof updateStatus === 'string' && updateStatus !== 'NOT_READY')) {
          try {
            const resp = await axios.post(`${PLAID_BASE}/transactions/get`, { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token, start_date: start, end_date: end }, { timeout: 20000 });
            // Cache and return
            ITEM_TRANSACTIONS[access_token] = resp.data;
            console.log(`Fetched and cached transactions after sync for access_token, total=${resp.data?.total_transactions || 0}`);
            return res.json(resp.data);
          } catch (e) {
            console.warn('transactions.get failed after sync indicated readiness', e?.response?.data || e?.message || e);
            // fallthrough to retry
          }
        }
      } catch (err) {
        console.warn('transactions.sync call failed', err?.response?.data || err?.message || err);
        // continue to retry
      }

      // backoff before next attempt (exponential with cap and jitter)
      const base = 1000 * (1 << Math.min(attempt - 1, 10)); // 1s,2s,4s,8s,16s,32s,64s... capped below
      const waitMs = Math.min(base, 60000) + Math.floor(Math.random() * 1000);
      console.log(`transactions_for_access_token: waiting ${waitMs}ms before next sync attempt (attempt ${attempt}/${maxAttempts})`);
      await new Promise(r => setTimeout(r, waitMs));
    }

    // If we get here, retries exhausted. If we have a lastSyncResp include it in error details to aid debugging.
    console.warn('No transactions after polling sync; returning last known sync response');
    return res.status(502).json({ error: 'transactions fetch failed - not ready', details: lastSyncResp || {} });
  } catch (err) {
    console.error('transactions.get error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'transactions fetch failed', details: err?.response?.data || err?.message });
  }
});

// New: transactions sync endpoint - calls Plaid's /transactions/sync to safely poll until transactions are available.
// /transactions/sync does not return PRODUCT_NOT_READY; it will return incremental sync results (may be empty).
app.post('/transactions_sync_for_access_token', requireDemoKey, async (req, res) => {
  try {
    const { access_token, cursor } = req.body;
    if (!access_token) return res.status(400).json({ error: 'missing access_token in body' });

    // Plaid /transactions/sync expects access_token and optionally a cursor to resume sync.
    const body = { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token };
    if (cursor) {
      body.cursor = cursor;
    }

    const resp = await axios.post(`${PLAID_BASE}/transactions/sync`, body, { timeout: 20000 });
    return res.json(resp.data);
  } catch (err) {
    console.error('transactions_sync_for_access_token error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'transactions sync failed', details: err?.response?.data || err?.message });
  }
});

// New: webhook handler for Plaid webhooks (DEMO ONLY)
// For real apps, verify the webhook signature and process securely.
// This demo simply logs and echoes the webhook data.
app.post('/webhook', async (req, res) => {
  try {
    const { webhook_code, access_token } = req.body;
    console.log('Webhook received:', req.body);

    // For demo, echo back the webhook code and access_token in the response.
    res.json({ webhook_code, access_token });
  } catch (err) {
    console.error('Webhook processing error:', err?.message || err);
    res.status(500).json({ error: 'webhook processing failed', details: err?.message });
  }
});

// Start the server (port 3000 for Replit, or use env PORT)
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Plaid demo server listening on port ${PORT}`);
});

