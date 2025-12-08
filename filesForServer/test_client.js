// filesForServer/test_client.js
// Small test client to call the local Plaid demo server endpoints.
// Usage: set env vars or create a .env file (copy .env.example -> .env), then run:
//   node test_client.js

const axios = require('axios');
require('dotenv').config();

const BACKEND = process.env.BACKEND_BASE || 'http://localhost:8000';
const DEMO_KEY = process.env.DEMO_API_KEY;
const PLAID_ENV = process.env.PLAID_ENV || 'sandbox';

if (!DEMO_KEY) {
  console.error('Please set DEMO_API_KEY in your environment or .env file (see .env.example)');
  process.exit(1);
}

async function createSandboxPublicToken(institutionId) {
  console.log('Requesting sandbox public_token...');
  const body = { institution_id: institutionId, initial_products: ['transactions'] };
  const resp = await axios.post(`${BACKEND}/create_sandbox_public_token`, body, { headers: { 'x-demo-key': DEMO_KEY } });
  return resp.data.public_token;
}

async function exchangePublicToken(publicToken) {
  console.log('Exchanging public_token for access_token...');
  const resp = await axios.post(`${BACKEND}/exchange_public_token`, { public_token: publicToken }, { headers: { 'x-demo-key': DEMO_KEY } });
  return resp.data; // contains access_token, item_id
}

async function fetchTransactions(accessToken) {
  console.log('Fetching transactions for access_token... (last 30 days by default)');
  const resp = await axios.post(`${BACKEND}/transactions_for_access_token`, { access_token: accessToken }, { headers: { 'x-demo-key': DEMO_KEY } });
  return resp.data;
}

async function run() {
  try {
    console.log('BACKEND:', BACKEND);
    console.log('PLAID_ENV:', PLAID_ENV);

    let publicToken = process.env.PUBLIC_TOKEN; // optional: you can set PUBLIC_TOKEN in .env to skip sandbox creation

    if (PLAID_ENV === 'sandbox' && !publicToken) {
      // Create a sandbox public_token using the server helper
      const sandboxInstitution = process.env.SANDBOX_INSTITUTION_ID || 'ins_109508';
      publicToken = await createSandboxPublicToken(sandboxInstitution);
      console.log('Sandbox public_token created:', publicToken);
    }

    if (!publicToken) {
      console.log('No public_token available. Exiting. To test exchange, set PUBLIC_TOKEN in .env or run the client after Link produces a public_token.');
      return;
    }

    const exchange = await exchangePublicToken(publicToken);
    console.log('Exchange response:');
    console.log(JSON.stringify(exchange, null, 2));

    const accessToken = exchange.access_token;
    if (!accessToken) {
      console.log('No access_token in exchange response — cannot fetch transactions.');
      return;
    }

    const tx = await fetchTransactions(accessToken);
    console.log('Transactions response:');
    console.log(JSON.stringify(tx, null, 2));

  } catch (err) {
    console.error('Error from server:', err?.response?.data || err.message || err);
  }
}

run();
