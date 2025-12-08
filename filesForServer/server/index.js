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

// Exchange public_token for access_token - mobile app POSTs public_token here
app.post('/exchange_public_token', requireDemoKey, async (req, res) => {
  try {
    const { public_token } = req.body;
    if (!public_token) return res.status(400).json({ error: 'missing public_token in body' });

    const body = {
      client_id: PLAID_CLIENT_ID,
      secret: PLAID_SECRET,
      public_token
    };

    const resp = await axios.post(`${PLAID_BASE}/item/public_token/exchange`, body, { timeout: 10000 });

    // FOR DEMO ONLY: returning access_token here so you can inspect it in the client.
    // DO NOT return access_token in a real production app. Instead store it securely server-side.
    res.json(resp.data);
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

    // default dates if not provided (last 30 days)
    const end = end_date || new Date().toISOString().slice(0, 10);
    const start = start_date || new Date(Date.now() - 1000 * 60 * 60 * 24 * 30).toISOString().slice(0, 10);

    const body = { client_id: PLAID_CLIENT_ID, secret: PLAID_SECRET, access_token, start_date: start, end_date: end };

    const resp = await axios.post(`${PLAID_BASE}/transactions/get`, body, { timeout: 20000 });
    res.json(resp.data);
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
    if (cursor) body.cursor = cursor;

    const resp = await axios.post(`${PLAID_BASE}/transactions/sync`, body, { timeout: 20000 });
    // resp.data will contain 'added', 'modified', 'removed' and 'next_cursor' etc. Return to client as-is.
    res.json(resp.data);
  } catch (err) {
    console.error('transactions.sync error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'transactions sync failed', details: err?.response?.data || err?.message });
  }
});

// Sandbox helper: create a sandbox public_token for testing without running Plaid Link.
// NOTE: This endpoint is intentionally restricted to PLAID_ENV === 'sandbox' to avoid misuse.
// Usage (POST): { "institution_id": "ins_109508", "initial_products": ["transactions"] }
// If institution_id is not provided, the server will try a common sandbox institution. If it fails,
// create a public_token using Plaid Link in the client instead.
app.post('/create_sandbox_public_token', requireDemoKey, async (req, res) => {
  if (PLAID_ENV !== 'sandbox') {
    return res.status(400).json({ error: 'create_sandbox_public_token is only available in sandbox mode' });
  }
  try {
    const institution_id = req.body.institution_id || 'ins_109508'; // common sandbox institution (may vary)
    const initial_products = req.body.initial_products || ['transactions'];

    const body = {
      client_id: PLAID_CLIENT_ID,
      secret: PLAID_SECRET,
      institution_id,
      initial_products
    };

    const resp = await axios.post(`${PLAID_BASE}/sandbox/public_token/create`, body, { timeout: 10000 });
    res.json(resp.data);
  } catch (err) {
    console.error('sandbox public_token create error:', err?.response?.data || err.message || err);
    res.status(502).json({ error: 'sandbox public_token creation failed', details: err?.response?.data || err?.message });
  }
});

const port = process.env.PORT || 5000;
app.listen(port, () => console.log(`Plaid demo server listening on http://0.0.0.0:${port} (PLAID_ENV=${PLAID_ENV})`));
