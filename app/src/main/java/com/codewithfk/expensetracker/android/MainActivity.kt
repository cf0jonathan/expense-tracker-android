package com.codewithfk.expensetracker.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.codewithfk.expensetracker.android.ui.theme.ExpenseTrackerAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On launch, if we have a saved Plaid access token, refresh transactions in the background
        try {
            val prefs = getSharedPreferences(PlaidLinkActivity.PREFS_NAME, MODE_PRIVATE)
            val accessToken = prefs.getString(PlaidLinkActivity.PREF_PLAID_ACCESS_TOKEN, null)
            if (!accessToken.isNullOrBlank()) {
                val intent = Intent(this, PlaidLinkActivity::class.java).apply {
                    action = PlaidLinkActivity.ACTION_REFRESH_ON_LAUNCH
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            // non-fatal — just log
            android.util.Log.w("MainActivity", "Failed to trigger Plaid refresh on launch", e)
        }

        setContent {
            ExpenseTrackerAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHostScreen()
                }
            }
        }
    }
}