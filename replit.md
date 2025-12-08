# Expense Tracker Android

## Overview
This is an Android expense tracking application built with Kotlin, Jetpack Compose, Room Database, Dagger Hilt, and MVVM architecture. The project includes an optional Plaid integration demo server for connecting bank accounts.

**Current State:** Project initialized on Replit with Node.js server ready for Plaid demo integration.

## Recent Changes
- 2025-12-08: Initial project setup on Replit
- 2025-12-08: Configured Node.js server workflow for Plaid demo backend

## Project Structure

### Android App (`/app`)
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Database:** Room (SQLite)
- **DI:** Dagger Hilt
- **Architecture:** MVVM

Main Features:
- Add and track expenses
- View expense lists
- Analyze spending with stats and charts
- Optional Plaid Link integration for bank account connectivity

### Backend Server (`/filesForServer`)
- **Language:** Node.js (Express)
- **Purpose:** Plaid Link demo server for sandbox testing
- **Port:** 8000 (internal), exposed on port 5000 for web access

Endpoints:
- `GET /` - Server info
- `GET /create_link_token` - Create Plaid link token (requires x-demo-key header)
- `POST /exchange_public_token` - Exchange public token for access token (requires x-demo-key header)
- `POST /transactions_for_access_token` - Fetch transactions demo (requires x-demo-key header)
- `POST /create_sandbox_public_token` - Create sandbox public token (sandbox only)

## Environment Variables

### Required for Plaid Integration
The following secrets are needed to use the Plaid demo server:
- `PLAID_CLIENT_ID` - Your Plaid client ID
- `PLAID_SECRET` - Your Plaid secret key
- `PLAID_ENV` - Plaid environment (sandbox/development/production, defaults to sandbox)
- `DEMO_API_KEY` - A custom API key for securing demo endpoints

These should be set in Replit Secrets (not committed to code).

## Getting Started

### Running the Backend Server
The Node.js server is configured to run automatically. It provides Plaid Link integration endpoints for the Android app.

### Android Development
This project is designed for Android Studio. The Replit environment is primarily for running the backend server. To develop the Android app:
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Configure Plaid integration (optional) by setting `plaidSdkVersion` in `gradle.properties`
5. Run on emulator or device

## User Preferences
(None set yet)

## Architecture Notes
- The backend server is separate from the Android app
- Plaid integration is optional and can be disabled
- Server uses Express with CORS enabled for cross-origin requests
- Demo API key authentication via `x-demo-key` header for basic security
