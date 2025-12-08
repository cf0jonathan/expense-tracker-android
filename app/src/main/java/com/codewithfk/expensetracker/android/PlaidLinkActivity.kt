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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
private const val BACKEND_BASE = "https://5c2c168e-d464-422f-8be9-93ee9eed9c2f-00-1vt53aj1y0ruo.spock.replit.dev" // <-- Replit URL
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
            val url = URL("$BACKEND_BASE/create_sandbox_public_token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
            }
            // minimal body: request transactions product
            val payload = "{\"initial_products\":[\"transactions\"]}"
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val respBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            }

            if (code !in 200..299) {
                Log.e(TAG, "create_sandbox_public_token failed: HTTP $code - $respBody")
                // Surface the server response to the UI for easier debugging
                withContext(Dispatchers.Main) {
                    plaidRuntimeStatus.value = "create_sandbox_public_token failed: HTTP $code - ${respBody ?: "<no body>"}"
                }
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
            val url = URL("$BACKEND_BASE/exchange_public_token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = "{\"public_token\":\"$publicToken\"}"
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val respBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            }

            if (code !in 200..299) {
                Log.e(TAG, "exchange_public_token failed: HTTP $code - $respBody")
                return@withContext null
            }

            // Parse JSON response and return access_token (demo server returns access_token in resp)
            try {
                val jobj = JSONObject(respBody)
                return@withContext if (jobj.has("access_token")) jobj.getString("access_token") else null
            } catch (je: Exception) {
                Log.e(TAG, "Failed to parse exchange response JSON", je)
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during exchange_public_token", e)
            null
        }
    }

    // Fetch transactions from backend using access_token and insert into Room DB. Returns number inserted.
    private suspend fun fetchAndStoreTransactions(accessToken: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BACKEND_BASE/transactions_for_access_token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-demo-key", DEMO_API_KEY)
                connectTimeout = 20000
                readTimeout = 20000
            }

            val payload = "{\"access_token\":\"$accessToken\"}"
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val respBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            }

            if (code !in 200..299) {
                Log.e(TAG, "transactions_for_access_token failed: HTTP $code - $respBody")
                return@withContext 0
            }

            // Parse transactions array
            val jobj = JSONObject(respBody)
            val transactions = if (jobj.has("transactions")) jobj.getJSONArray("transactions") else JSONArray()

            var inserted = 0
            for (i in 0 until transactions.length()) {
                val t = transactions.getJSONObject(i)
                val name = t.optString("name", "Plaid Transaction")
                val amount = t.optDouble("amount", 0.0)
                val date = t.optString("date", "")
                // For demo, treat positive amounts as Expense
                val type = if (amount >= 0) "Expense" else "Income"

                val entity = ExpenseEntity(id = null, title = name, amount = kotlin.math.abs(amount), date = date, type = type)
                try {
                    expenseDao.insertExpense(entity)
                    inserted++
                } catch (ie: Exception) {
                    Log.e(TAG, "Failed to insert expense entity", ie)
                }
            }

            inserted
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
            0
        }
    }

    private suspend fun fetchLinkToken(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BACKEND_BASE/create_link_token")
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
                // Surface the server response to the UI for easier debugging
                withContext(Dispatchers.Main) {
                    plaidRuntimeStatus.value = "create_link_token failed: HTTP $code - ${err ?: "<no body>"}"
                }
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            // quick parse for link_token: { "link_token": "..." }
            val token = Regex("\"link_token\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groups?.get(1)?.value
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching link token", e)
            withContext(Dispatchers.Main) {
                plaidRuntimeStatus.value = "Error fetching link token: ${e.message}"
            }
            null
        }
    }
}
