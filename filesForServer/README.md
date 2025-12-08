Plaid demo server (filesForServer)

Purpose
- Minimal Express server to demo Plaid Link flow using Plaid sandbox.
- Intended for local testing or deployment to Replit/Glitch/Fly/Railway for a small demo.

Important: never commit real secrets. Use the host's environment variable mechanism (Replit Secrets, Railway env, etc.).

Required environment variables
- PLAID_CLIENT_ID
- PLAID_SECRET
- PLAID_ENV (defaults to "sandbox")
- DEMO_API_KEY (set a secret value and share it with your professor)

Endpoints
- GET / (info)
- GET /create_link_token  (requires header x-demo-key)
- POST /exchange_public_token  (requires header x-demo-key, body: { public_token })
- POST /transactions_for_access_token (demo only) (requires header x-demo-key, body: { access_token, start_date?, end_date? })

Replit quick start
1. Create a new Repl -> "Import from GitHub" (or create a new Node repl and paste these files).
2. Add Secrets in Replit: PLAID_CLIENT_ID, PLAID_SECRET, PLAID_ENV (sandbox), DEMO_API_KEY
3. Run the repl. Replit will expose an HTTPS URL like: https://<replname>.<username>.repl.co
4. In your Android app, set backendBase to the Replit URL and include header x-demo-key with DEMO_API_KEY on requests.

Local quick start (Windows cmd.exe)
1. cd filesForServer
2. npm install
3. set PLAID_CLIENT_ID=your_id
   set PLAID_SECRET=your_secret
   set PLAID_ENV=sandbox
   set DEMO_API_KEY=some-demo-key
4. npm start
5. Server runs on http://localhost:8000

Notes
- This demo returns the Plaid access_token in the /exchange_public_token response for convenience only. Never return access tokens to real clients in production.
- Consider adding rate-limiting and logging for better demo resilience.

