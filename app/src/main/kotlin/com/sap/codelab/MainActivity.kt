package com.sap.codelab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.ui.components.navigation.AppNavigation
import com.sap.codelab.ui.components.theme.CodelabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** The memo a tapped reminder asks to open; consumed by AppNavigation. */
    private var deepLinkMemoId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // After a recreation the back stack is restored and already contains the memo.
        if (savedInstanceState == null) deepLinkMemoId = intent.memoId()
        setContent {
            CodelabTheme {
                AppNavigation(deepLinkMemoId = deepLinkMemoId, onDeepLinkHandled = { deepLinkMemoId = null })
            }
        }
    }

    /** A reminder tapped while the app is open (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.memoId()?.let { deepLinkMemoId = it }
    }
}

private fun Intent.memoId(): Long? = getLongExtra(ReminderNotifier.EXTRA_MEMO_ID, -1L).takeIf { it > 0 }
