package com.sap.codelab.android

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.MainActivity
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.data.notification.ReminderNotifierImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ReminderNotifierImplTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifier = ReminderNotifierImpl(app)

    @Before
    fun registerLauncher() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app.packageName)
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = app.packageName
                name = MainActivity::class.java.name
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(launcher, info)
    }

    @Test
    fun shows_a_reminder_that_opens_the_memo() {
        assertEquals(AppResult.Success(Unit), notifier.showReminder(7, "Buy milk", "At the corner shop"))

        val notification = shadowOf(notificationManager).getNotification(7)
        assertEquals("Buy milk", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("At the corner shop", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("memo_reminders", notification.channelId)
        assertTrue(notificationManager.getNotificationChannel("memo_reminders") != null)
        val opened = shadowOf(notification.contentIntent).savedIntent
        assertEquals(7L, opened.getLongExtra(ReminderNotifier.EXTRA_MEMO_ID, -1))
    }

    @Test
    fun blocked_notifications_are_reported() {
        shadowOf(notificationManager).setNotificationsEnabled(false)
        val result = notifier.showReminder(7, "Buy milk", "text")
        assertTrue((result as AppResult.Failure).error is AppError.NotificationsBlocked)
        assertNull(shadowOf(notificationManager).getNotification(7))
    }
}
