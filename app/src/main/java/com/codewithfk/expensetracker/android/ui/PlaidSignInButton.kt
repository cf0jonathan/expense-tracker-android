package com.codewithfk.expensetracker.android.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.codewithfk.expensetracker.android.PlaidLinkActivity

@Composable
fun PlaidSignInButton() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PlaidLinkActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    var signedIn by remember { mutableStateOf(prefs.getBoolean(PlaidLinkActivity.PREF_PLAID_SIGNED_IN, false)) }

    // Keep state in sync with SharedPreferences changes
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == PlaidLinkActivity.PREF_PLAID_SIGNED_IN) {
                signedIn = sp.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // If already signed in, render nothing (button disappears)
    if (signedIn) return

    Button(onClick = {
        val intent = Intent(context, PlaidLinkActivity::class.java).apply {
            action = PlaidLinkActivity.ACTION_FETCH_AND_OPEN
            // don't add NEW_TASK so finish() returns to the calling activity (Home)
        }
        context.startActivity(intent)
    }) {
        Text(text = "Sign in with Plaid")
    }
}
