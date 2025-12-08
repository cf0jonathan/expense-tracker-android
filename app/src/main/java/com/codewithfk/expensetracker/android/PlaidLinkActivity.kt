package com.codewithfk.expensetracker.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import com.codewithfk.expensetracker.android.data.dao.ExpenseDao
import com.codewithfk.expensetracker.android.data.model.ExpenseEntity
import org.json.JSONObject
import org.json.JSONArray

// PlaidLinkActivity
// - Fetches a link_token from your backend and displays it for testing.
// - Includes a TODO and example snippet showing where to put Plaid Link SDK launch code.
// How to use:
// 1) Update BACKEND_BASE and DEMO_API_KEY below to point at your running server (or keep 10.0.2.2:8000 for emulator).
// 2) Run the activity and press "Fetch Link Token" to retrieve a link_token from your server.
// 3) Add Plaid Link SDK dependency to your app (see commented snippet) and replace the TODO code in onOpenLinkClick()
//    with the SDK call to open Plaid Link using the returned link token.

private const val TAG = "PlaidLinkActivity"

// TODO: set these values for local testing. For emulator use http://10.0.2.2:8000
// Replit backend URL provided by you:
private const val BACKEND_BASE = "https://8662df20-18fc-4262-828b-6bcc97648a5e-00-10mhwkj9qom2o.janeway.replit.dev" // <-- Replit URL (no trailing slash)

// Helper to join paths safely to BACKEND_BASE (avoids double-slashes and makes logs readable)
private fun joinUrl(path: String): String = BACKEND_BASE.trimEnd('/') + "/${path.trimStart('/')}"
private const val DEMO_API_KEY = "totallySecureDemoKeyForProjectAtUALR" // <-- replace with your DEMO_API_KEY or wire from BuildConfig/gradle property

@AndroidEntryPoint
class PlaidLinkActivity : ComponentActivity() {
    @Inject
    lateinit var expenseDao: ExpenseDao

    // Expose a small Compose-friendly runtime status so the UI can show errors or SDK state.
    private val plaidRuntimeStatus = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Previously this registered a Plaid callback that referenced Plaid SDK classes directly.
        // That caused compile-time errors when the optional Plaid dependency wasn't added.
        // Keep a safe runtime check + message here and avoid direct type references.
        if (BuildConfig.PLAID_SDK_ENABLED) {
            Log.i(TAG, "Plaid SDK enabled in this build. Make sure you added the SDK dependency and implement result handling.")
            plaidRuntimeStatus.value = "Plaid SDK enabled"
            // If you add the Plaid SDK dependency, implement result callbacks here (or use the reflection approach below to keep this file free of direct SDK types).
        } else {
            Log.w(TAG, "Plaid SDK not enabled in this build. Set plaidSdkVersion in gradle.properties to enable it.")
            plaidRuntimeStatus.value = "Plaid SDK NOT enabled"
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaidLinkScreen()
                }
            }
        }
    }

    @Composable
    fun PlaidLinkScreen() {
        var linkToken by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }
        var lastError by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Plaid Link Demo", style = MaterialTheme.typography.titleLarge)

            // Visible runtime status for Plaid SDK and reflection/errors
            Text(text = "Plaid status: ${plaidRuntimeStatus.value ?: "unknown"}", modifier = Modifier.padding(top = 8.dp))

            Button(onClick = {
                // fetch link token from backend
                lifecycleScope.launch {
                    loading = true
                    lastError = null
                    val token = fetchLinkToken()
                    linkToken = token
                    loading = false

                    // Auto-open Plaid Link immediately when a token is fetched and the SDK is enabled.
                    if (!token.isNullOrBlank() && BuildConfig.PLAID_SDK_ENABLED) {
                        try {
                            onOpenLinkClick(token)
                        } catch (e: Exception) {
                            Log.e(TAG, "Auto-open Plaid failed", e)
                            plaidRuntimeStatus.value = "Auto-open failed: ${e.message}"
                        }
                    }
                }
            }, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "Fetch Link Token")
            }

            // NEW: simulation button to test the full server -> exchange -> transactions path without Plaid UI
            Button(onClick = {
                lifecycleScope.launch {
                    plaidRuntimeStatus.value = "Simulating sandbox flow..."
                    try {
                        val publicToken = createSandboxPublicToken()
                        if (publicToken.isNullOrBlank()) {
                            plaidRuntimeStatus.value = "Failed to create sandbox public_token"
                            return@launch
                        }
                        plaidRuntimeStatus.value = "Got sandbox public_token"

                        val accessToken = exchangePublicToken(publicToken)
                        if (accessToken.isNullOrBlank()) {
                            plaidRuntimeStatus.value = "Failed to exchange public_token"
                            return@launch
                        }
                        plaidRuntimeStatus.value = "Got access_token"

                        val count = fetchAndStoreTransactions(accessToken)
                        plaidRuntimeStatus.value = "Inserted $count transactions from sandbox"

                        // Immediately dump DB rows after simulation to make debugging easier
                        try {
                            val rows = withContext(Dispatchers.IO) { expenseDao.getAllExpensesList() }
                            Log.d(TAG, "Post-sim DB rows dump (count=${rows.size}): $rows")
                            val preview = rows.take(5).joinToString(separator = "\n") { it.toString() }
                            plaidRuntimeStatus.value = "Inserted $count transactions from sandbox\nDB rows: count=${rows.size}\n$preview"
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to dump DB rows after simulation", e)
                        }
                    } catch (e: Exception) {
                        plaidRuntimeStatus.value = "Sandbox simulation error: ${e.message}"
                    }
                }
            }, modifier = Modifier.padding(top = 12.dp)) {
                Text(text = "Simulate Sandbox Flow (no Plaid UI)")
            }

            // NEW: local-only demo insertion to allow immediate testing without any backend or Plaid credentials
            Button(onClick = {
                lifecycleScope.launch {
                    plaidRuntimeStatus.value = "Inserting demo transactions locally..."
                    val count = insertDemoTransactionsLocal()
                    plaidRuntimeStatus.value = "Inserted $count demo transactions locally"
                }
            }, modifier = Modifier.padding(top = 12.dp)) {
                Text(text = "Insert Demo Transactions (local)")
            }

            // NEW: dump DB rows to logs and show a summary count & first rows in the status
            Button(onClick = {
                lifecycleScope.launch {
                    plaidRuntimeStatus.value = "Dumping DB rows..."
                    try {
                        val rows = withContext(Dispatchers.IO) { expenseDao.getAllExpensesList() }
                        Log.d(TAG, "DB rows dump (count=${rows.size}): $rows")
                        val preview = rows.take(5).joinToString(separator = "\n") { it.toString() }
                        plaidRuntimeStatus.value = "DB rows: count=${rows.size}\n$preview"
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to dump DB rows", e)
                        plaidRuntimeStatus.value = "Failed to dump DB rows: ${e.message}"
                    }
                }
            }, modifier = Modifier.padding(top = 12.dp)) {
                Text(text = "Dump DB Rows (debug)")
            }

            if (loading) Text(text = "Loading...")

            linkToken?.let { token ->
                Text(text = "Link token:\n$token", modifier = Modifier.padding(top = 16.dp))

                Button(onClick = {
                    onOpenLinkClick(token)
                }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = "Open Plaid Link (TODO)")
                }
            }

            lastError?.let { err ->
                Text(text = "Error: $err", modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    // Local helper that inserts a few sample ExpenseEntity rows into Room for UI testing.
    private suspend fun insertDemoTransactionsLocal(): Int = withContext(Dispatchers.IO) {
        val demo = listOf(
            ExpenseEntity(id = null, title = "Demo Coffee", amount = 4.50, date = "2025-12-07", type = "Expense"),
            ExpenseEntity(id = null, title = "Demo Groceries", amount = 32.75, date = "2025-12-05", type = "Expense"),
            ExpenseEntity(id = null, title = "Demo Salary", amount = 1500.00, date = "2025-12-01", type = "Income")
        )
        var inserted = 0
        for (e in demo) {
            try {
                expenseDao.insertExpense(e)
                inserted++
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to insert demo expense", ex)
            }
        }
        inserted
    }

    // Helper to call the server sandbox public_token endpoint
    private suspend fun createSandboxPublicToken(): String? = withContext(Dispatchers.IO) {
        try {
            val urlStr = joinUrl("create_sandbox_public_token")
            val payload = "{\"initial_products\":[\"transactions\"]}"
            Log.d(TAG, "POST $urlStr payload=$payload")

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
            }

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val respBody = if (code in 200..299) conn.inputStream.bufferedReader().use(BufferedReader::readText) else conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)

            Log.d(TAG, "create_sandbox_public_token HTTP $code response=${respBody?.take(2000)}")
            if (code !in 200..299) {
                withContext(Dispatchers.Main) {
                    plaidRuntimeStatus.value = "create_sandbox_public_token failed: HTTP $code - ${respBody ?: "<no body>"}"
                }
                return@withContext null
            }

            if (respBody.isNullOrBlank()) {
                Log.e(TAG, "create_sandbox_public_token empty response body")
                withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "create_sandbox_public_token returned empty body" }
                return@withContext null
            }

            val jobj = JSONObject(respBody)
            return@withContext if (jobj.has("public_token")) jobj.getString("public_token") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sandbox public_token", e)
            withContext(Dispatchers.Main) {
                plaidRuntimeStatus.value = "Error creating sandbox public_token: ${e.message}"
            }
            null
        }
    }

    private fun onOpenLinkClick(token: String) {
        if (!BuildConfig.PLAID_SDK_ENABLED) {
            val msg = "Plaid SDK not enabled in this build. Set plaidSdkVersion in gradle.properties to enable it."
            Log.w(TAG, msg)
            plaidRuntimeStatus.value = msg
            return
        }

        try {
            // Use the Plaid SDK directly (the project adds the SDK when `plaidSdkVersion` is set in gradle.properties).
            // Keep fully-qualified names to avoid import issues if the SDK isn't present at compile time.
            val config = com.plaid.link.configuration.LinkTokenConfiguration.Builder()
                .token(token)
                .build()

            // Pass the Application instance (required by Plaid SDK) instead of applicationContext.
            com.plaid.link.Plaid.create(this.application, config).open(this)

            plaidRuntimeStatus.value = "Plaid Link launched"
        } catch (e: java.lang.NoClassDefFoundError) {
            Log.e(TAG, "Plaid SDK classes not available at runtime", e)
            plaidRuntimeStatus.value = "Plaid SDK classes not available at runtime"
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Plaid Link", e)
            plaidRuntimeStatus.value = "Error launching Plaid Link: ${e.message}"
        }
    }

    // Call this method from your Plaid Link success callback with the public_token returned by Plaid.
    fun handlePublicToken(publicToken: String) {
        // Exchanges the public_token for an access_token by calling your backend.
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Handling public_token: $publicToken")
                val accessToken = exchangePublicToken(publicToken)
                Log.d(TAG, "access_token: $accessToken")
                if (!accessToken.isNullOrBlank()) {
                    // Fetch transactions from backend and insert into local DB
                    val count = fetchAndStoreTransactions(accessToken)
                    Log.d(TAG, "Inserted $count transactions from Plaid into local DB")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging public_token", e)
            }
        }
    }

    private suspend fun exchangePublicToken(publicToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val urlStr = joinUrl("exchange_public_token")
            val payload = "{\"public_token\":\"$publicToken\"}"
            Log.d(TAG, "POST $urlStr payload={public_token:REDACTED}")

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
            }

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val respBody = if (code in 200..299) conn.inputStream.bufferedReader().use(BufferedReader::readText) else conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)

            Log.d(TAG, "exchange_public_token HTTP $code response=${respBody?.take(2000)}")
            if (code !in 200..299) {
                withContext(Dispatchers.Main) {
                    plaidRuntimeStatus.value = "exchange_public_token failed: HTTP $code - ${respBody ?: "<no body>"}"
                }
                return@withContext null
            }

            if (respBody.isNullOrBlank()) {
                Log.e(TAG, "exchange_public_token empty response body")
                withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "exchange_public_token returned empty body" }
                return@withContext null
            }

            val jobj = try { JSONObject(respBody) } catch (je: Exception) {
                Log.e(TAG, "Failed to parse exchange response JSON", je)
                withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Invalid JSON from exchange_public_token" }
                return@withContext null
            }

            return@withContext if (jobj.has("access_token")) jobj.getString("access_token") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error during exchange_public_token", e)
            withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Error during exchange_public_token: ${e.message}" }
            null
        }
    }

    // Fetch transactions from backend using access_token and insert into Room DB. Returns number inserted.
    private suspend fun fetchAndStoreTransactions(accessToken: String): Int = withContext(Dispatchers.IO) {
        // Retry loop: Plaid can return PRODUCT_NOT_READY in sandbox immediately after creating an item.
        // Use a longer, production-safe schedule: more attempts, cap at 60s, and add jitter up to 1s.
        val maxAttempts = 10
        var attempt = 0
        var lastRespBody: String? = null
        var lastCode = -1
        var transactions = JSONArray()

        while (attempt < maxAttempts) {
            attempt++
            try {
                // First try: call backend's transactions_sync_for_access_token which proxies Plaid /transactions/sync
                val syncUrl = joinUrl("transactions_sync_for_access_token")
                val syncPayload = "{\"access_token\":\"$accessToken\"}"
                Log.d(TAG, "POST $syncUrl payload={access_token:REDACTED} attempt=$attempt")

                val syncUrlObj = URL(syncUrl)
                val syncConn = (syncUrlObj.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-demo-key", DEMO_API_KEY)
                    connectTimeout = 20000
                    readTimeout = 20000
                }

                syncConn.outputStream.use { os ->
                    os.write(syncPayload.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                var code = syncConn.responseCode
                var respBody = if (code in 200..299) syncConn.inputStream.bufferedReader().use(BufferedReader::readText) else syncConn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                lastCode = code
                lastRespBody = respBody

                Log.d(TAG, "transactions_sync_for_access_token HTTP $code response=${respBody?.take(2000)}")

                var syncHasAdded = false
                if (code in 200..299 && !respBody.isNullOrBlank()) {
                    try {
                        val jobj = JSONObject(respBody)
                        val added = jobj.optJSONArray("added")
                        if (added != null && added.length() > 0) {
                            // Convert 'added' array into the transactions JSONArray expected by downstream code.
                            transactions = JSONArray()
                            for (i in 0 until added.length()) {
                                transactions.put(added.getJSONObject(i))
                            }
                            syncHasAdded = true
                        }
                    } catch (je: Exception) {
                        Log.w(TAG, "Failed to parse sync response JSON", je)
                    }
                }

                if (syncHasAdded) {
                    Log.d(TAG, "transactions.sync returned ${transactions.length()} added transactions; proceeding to insert")
                    break
                }

                // If sync returned no added items, fall back to GET transactions endpoint and continue retry logic.
                val urlStr = joinUrl("transactions_for_access_token")
                val payload = "{\"access_token\":\"$accessToken\"}"
                Log.d(TAG, "POST $urlStr payload={access_token:REDACTED} (fallback) attempt=$attempt")

                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-demo-key", DEMO_API_KEY)
                    connectTimeout = 20000
                    readTimeout = 20000
                }

                conn.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                code = conn.responseCode
                lastCode = code
                respBody = if (code in 200..299) conn.inputStream.bufferedReader().use(BufferedReader::readText) else conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                lastRespBody = respBody

                Log.d(TAG, "transactions_for_access_token HTTP $code response=${respBody?.take(2000)}")

                if (code in 200..299) {
                    if (respBody.isNullOrBlank()) {
                        Log.e(TAG, "transactions_for_access_token returned empty body on success")
                        // treat as no-data and retry
                    } else {
                        val jobj = try { JSONObject(respBody) } catch (je: Exception) {
                            Log.e(TAG, "Failed to parse transactions JSON", je)
                            withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Invalid JSON from transactions_for_access_token" }
                            return@withContext 0
                        }
                        transactions = if (jobj.has("transactions")) jobj.getJSONArray("transactions") else JSONArray()
                        // success, break retry loop
                        break
                    }
                } else {
                    // non-2xx: inspect Plaid error body for PRODUCT_NOT_READY and retry if so
                    try {
                        val errJ = if (!respBody.isNullOrBlank()) JSONObject(respBody) else null
                        val errorCode = errJ?.optString("error_code") ?: errJ?.optJSONObject("details")?.optString("error_code")
                        val errorType = errJ?.optString("error_type") ?: errJ?.optJSONObject("details")?.optString("error_type")
                        Log.w(TAG, "transactions_for_access_token error code=$errorCode type=$errorType")
                        if (errorCode == "PRODUCT_NOT_READY") {
                            // exponential backoff with cap and random jitter to avoid synchronized retries
                            val base = 1000L * (1L shl (attempt - 1)) // 1s,2s,4s...
                            val waitMs = base.coerceAtMost(60000L) + Random.nextLong(0, 1000)
                            Log.i(TAG, "PRODUCT_NOT_READY, will retry after ${waitMs}ms (attempt $attempt/$maxAttempts)")
                            withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Transactions not ready, retrying... (attempt $attempt)" }
                            delay(waitMs)
                            continue
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to inspect error body for retry", e)
                    }
                    // Other errors: surface and stop
                    withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "transactions_for_access_token failed: HTTP $code - ${respBody ?: "<no body>"}" }
                    return@withContext 0
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during transactions_for_access_token attempt $attempt", e)
                if (attempt >= maxAttempts) {
                    withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Error fetching transactions: ${e.message}" }
                    return@withContext 0
                }
                // use the same capped backoff + jitter for exceptions
                val base = 1000L * (1L shl (attempt - 1))
                val waitMs = base.coerceAtMost(60000L) + Random.nextLong(0, 1000)
                Log.i(TAG, "Transient error, will retry after ${waitMs}ms (attempt $attempt/$maxAttempts)")
                delay(waitMs)
                continue
            }
        }

        // If we exhausted retries and still have no transactions, surface the last response
        if (transactions.length() == 0) {
            Log.w(TAG, "No transactions after $maxAttempts attempts, lastCode=$lastCode lastResp=${lastRespBody?.take(2000)}")
            withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "No transactions: last HTTP $lastCode - ${lastRespBody ?: "<no body>"}" }
            return@withContext 0
        }

        // Debug: log parsed transactions count and a sample
        Log.d(TAG, "Parsed transactions count=${transactions.length()}")
        if (transactions.length() > 0) {
            try {
                Log.d(TAG, "First transaction JSON: ${transactions.getJSONObject(0).toString()}" )
            } catch (e: Exception) {
                Log.w(TAG, "Could not log first transaction JSON", e)
            }
        }

        // Debug: capture count before inserts
        val beforeCount = try { expenseDao.countExpenses() } catch (e: Exception) { Log.w(TAG, "countExpenses before failed", e); -1 }
        Log.d(TAG, "DB count before inserts = $beforeCount")

        var inserted = 0
        for (i in 0 until transactions.length()) {
            val t = transactions.getJSONObject(i)
            val name = t.optString("name", "Plaid Transaction")
            val amount = t.optDouble("amount", 0.0)
            val date = t.optString("date", "")
            val type = if (amount >= 0) "Expense" else "Income"

            val entity = ExpenseEntity(id = null, title = name, amount = kotlin.math.abs(amount), date = date, type = type)
            try {
                Log.d(TAG, "Attempting to insert expense from Plaid: title=${entity.title}, amount=${entity.amount}, date=${entity.date}, type=${entity.type}")
                expenseDao.insertExpense(entity)
                Log.d(TAG, "Inserted expense into DB: $entity")
                inserted++
            } catch (ie: Exception) {
                Log.e(TAG, "Failed to insert expense entity", ie)
            }
        }

        // Debug: capture count after inserts
        val afterCount = try { expenseDao.countExpenses() } catch (e: Exception) { Log.w(TAG, "countExpenses after failed", e); -1 }
        Log.d(TAG, "DB count after inserts = $afterCount")
        withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Fetched ${transactions.length()} txns, inserted $inserted (db before=$beforeCount after=$afterCount)" }
        inserted
    }

    private suspend fun fetchLinkToken(): String? = withContext(Dispatchers.IO) {
        try {
            val urlStr = joinUrl("create_link_token")
            Log.d(TAG, "GET $urlStr")
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Log.e(TAG, "create_link_token failed: HTTP $code - $err")
                withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "create_link_token failed: HTTP $code - ${err ?: "<no body>"}" }
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            Log.d(TAG, "create_link_token response=${body.take(2000)}")
            // quick parse for link_token: { "link_token": "..." }
            val token = Regex("\"link_token\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groups?.get(1)?.value
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching link token", e)
            withContext(Dispatchers.Main) { plaidRuntimeStatus.value = "Error fetching link token: ${e.message}" }
            null
        }
    }
}
