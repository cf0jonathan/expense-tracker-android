// filesForServer/test_client.js
// Small test client to call the local Plaid demo server endpoints.
// Usage: set env vars or create a .env file (copy .env.example -> .env), then run:
//   node test_client.js

const axios = require('axios');
require('dotenv').config();

const BACKEND = process.env.BACKEND_BASE || 'http://localhost:8000';
const DEMO_KEY = process.env.DEMO_API_KEY;

if (!DEMO_KEY) {
  console.error('Please set DEMO_API_KEY in your environment or .env file (see .env.example)');
  process.exit(1);
}

async function run() {
  try {
    console.log('Calling GET /create_link_token');
    const resp = await axios.get(`${BACKEND}/create_link_token`, { headers: { 'x-demo-key': DEMO_KEY } });
    console.log('create_link_token response:');
    console.log(JSON.stringify(resp.data, null, 2));

    // If you have a sandbox public_token you can test exchange. For demo, you can skip.
    // Example public_token (sandbox) is usually returned by Plaid Link. If you have one, uncomment below.
    // const publicToken = '<public-sandbox-...>';
    // const exch = await axios.post(`${BACKEND}/exchange_public_token`, { public_token: publicToken }, { headers: { 'x-demo-key': DEMO_KEY } });
    // console.log('exchange_public_token response:');
    // console.log(JSON.stringify(exch.data, null, 2));

  } catch (err) {
    console.error('Error from server:', err?.response?.data || err.message || err);
  }
}

run();

