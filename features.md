# Features

## Summary
ExpenseTracker is a lightweight personal finance app that tracks income, expenses, balances, budgets, and linked bank transactions via Plaid. Key UX elements include a Home dashboard, recent transactions list, add expense/income flows, and account linking.

## Navigation / Login
- Automatic return to Home after successful app login and after completing Plaid linking (flow returns user to Home).
- Multi-FAB for quick add actions (expense / income).
- "See all" to view full transactions list.

## Dashboard
- Total Balance card showing:
    - Current balance (income - expense)
    - Total income and total expense (period totals)
- Quick visual of income vs expense.

## Transactions
- Recent Transactions list on Home (click "See all" for full list).
- Each transaction shows:
    - Icon (category)
    - Title (UI truncation applied)
    - Date (formatted as Month Day, Year)
    - Amount with sign/color (green for income, red for expense)
- Actions:
    - Delete transaction via menu
    - (Planned) Edit transaction entry

UI truncation details:
- Long titles are truncated on the app (single-line ellipsis) to prevent layout breakage.
- Additional truncation/validation should be enforced at import/storage (recommend limiting title length on import/save, e.g., 80 characters).

## Budgeting (focused)
- Create budgets scoped by category and/or monthly period.
- Track actual spending vs budgeted amount per category.
- Live budget status shown on Dashboard / Budgets screen:
    - Current spend, remaining amount, percentage used.
- Notifications:
    - Warn when spending approaches a configured threshold (e.g., 80%).
    - Alert on budget exceeded.
- Reconciliation:
    - Imported transactions (Plaid or manual) are categorized and counted against budgets automatically.
- Calculation details:
    - Budget progress and alerts are derived from aggregated transactions for the selected period (month by default) using the same currency/formatting utilities as the main totals.
- Persistence:
    - Budgets saved in app storage and evaluated whenever transactions change or on a periodic sync.
- Suggested implementation notes:
    - Enforce category mapping on import to attribute income/expense to budgets.
    - Provide a budget edit/create UI with period, amount, categories, and alert thresholds.

## Plaid Integration (heavy)
- Plaid account linking initiated via a Plaid sign-in button (launches PlaidLinkActivity).
- Post-link flow:
    - On success, linked accounts and transactions are imported and the user is returned to Home.
- Imported data includes:
    - Transaction title/merchant name
    - Amount (signed as debit/credit)
    - Transaction date (stored and displayed; formatted for the UI)
    - Category / merchant metadata (when available)
    - Transaction id / reference for deduplication
- Import behavior:
    - Batch import of recent transactions from linked accounts.
    - Deduplication using transaction id and date to avoid duplicates on subsequent syncs.
    - Map Plaid categories to app categories; fall back to manual categorization when ambiguous.
    - Trim/limit long merchant names before saving (to avoid layout/db issues).
- Error handling & sync:
    - Graceful failure with retry/logging on network or Plaid errors.
    - Option to manually trigger a sync.
- Security:
    - Plaid tokens and sensitive credentials handled per best practices (not stored insecurely).
- Notes:
    - Dates are captured from Plaid and formatted via the app's date utilities for display (e.g., `Utils.formatStringDateToMonthDayYear`).

## Statistics & Analytics
- Overview charts and statistics (monthly totals, category breakdowns).
- Current status: basic totals and category aggregation; extendable to charts and trends.
- Stats are recomputed from stored transactions and budgets.

## Add / Edit Expense & Income
- Multi-FAB expands to add income or expense.
- Add forms capture: title, amount, date, category, notes.
- Amounts formatted consistently across app.

## Utilities & Formatting
- Currency formatting utilities used across Dashboard and lists.
- Date formatting utilities ensure consistent display.
- Icons and category mapping utilities support transaction visuals.

## Developer / Implementation Notes
- UI truncation for titles: implemented at `TransactionItem` (single-line ellipsis).
- Budget logic should be kept in viewmodels/repository so it updates reactively when transactions change.
- Ensure Plaid imports capture and store `date` fields (used by statistics and lists).
- Recommend enforcing server/db-side or repository-level title length limits to complement UI truncation.

## Backend / Server (brief)
- Role: the server handles sensitive Plaid operations and persistent storage that must not live in the client.
- Typical responsibilities:
  - Create Plaid Link tokens (server endpoint invoked by the app to begin the link flow).
  - Exchange the client-side public_token for an access_token via Plaid's /item/public_token/exchange and store the resulting access_token/item_id securely on the server.
  - Fetch transactions from Plaid (/transactions/get) and return them to the client or persist them in the app's database.
  - Deduplicate incoming transactions (use Plaid transaction id + date) and map Plaid categories to app categories before saving.
  - Expose minimal APIs to the client (for example: POST /plaid/create_link_token, POST /plaid/exchange_public_token, POST /plaid/sync, GET /transactions).
  - Schedule background syncs or accept Plaid webhooks to keep transactions up-to-date.
- Security & ops:
  - Never store Plaid access tokens in the client. Store them encrypted on the server with access control.
  - Use server-side rate limiting, logging, and error handling for Plaid API calls.
  - For development, use Plaid sandbox/demo credentials and test accounts; for production use Plaid's recommended best practices.

*Note: this section intentionally keeps the backend high-level since the app's core focus is the mobile UX and budgeting features.*
