package com.sap.codelab.data.notification

import com.sap.codelab.core.AppResult

/** Shows memo reminders in the status bar. Tapping one opens the app with [EXTRA_MEMO_ID]. */
interface ReminderNotifier {
    /** [text] is shown as given; the caller shortens it. Fails if notifications are not allowed. */
    fun showReminder(
        memoId: Long,
        title: String,
        text: String,
    ): AppResult<Unit>

    companion object {
        /** The launch intent extra carrying the memo to open (read by MainActivity). */
        const val EXTRA_MEMO_ID = "com.sap.codelab.extra.MEMO_ID"
    }
}
