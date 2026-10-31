package com.sap.codelab.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sap.codelab.R
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

private const val TAG = "Reminder"
private const val CHANNEL_ID = "memo_reminders"

internal class ReminderNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    override fun showReminder(
        memoId: Long,
        title: String,
        text: String,
    ): AppResult<Unit> {
        if (!notificationManager.areNotificationsEnabled()) {
            Timber.tag(TAG).w("Notifications are disabled; reminder not shown")
            return AppResult.Failure(AppError.NotificationsBlocked(null))
        }
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openMemoIntent(memoId))
            .setAutoCancel(true)
            .build()
        return try {
            notificationManager.notify(memoId.toInt(), notification)
            AppResult.Success(Unit)
        } catch (e: SecurityException) { // POST_NOTIFICATIONS revoked between the check and notify
            Timber.tag(TAG).w(e, "Reminder not shown")
            AppResult.Failure(AppError.NotificationsBlocked(e))
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_reminders))
            .setDescription(context.getString(R.string.notification_channel_reminders_description))
            .build()
        notificationManager.createNotificationChannel(channel) // no-op when it already exists
    }

    /** The launcher intent, so the data layer never references MainActivity. */
    private fun openMemoIntent(memoId: Long): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?.putExtra(ReminderNotifier.EXTRA_MEMO_ID, memoId)
            ?: return null
        return PendingIntent.getActivity(
            context,
            memoId.toInt(), // one request code per memo, so each notification keeps its own extra
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
