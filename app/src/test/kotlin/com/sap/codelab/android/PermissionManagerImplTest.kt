package com.sap.codelab.android

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.data.permission.PermissionManagerImpl
import com.sap.codelab.data.permission.PermissionsData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class PermissionManagerImplTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = PermissionManagerImpl(app)

    private fun grant(vararg permissions: String) = shadowOf(app).grantPermissions(*permissions)

    @Test
    fun nothing_granted() {
        assertEquals(PermissionsData(fineLocation = false, coarseLocation = false, backgroundLocation = false, notifications = false), manager.currentPermissions())
    }

    @Test
    fun everything_granted() {
        grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        assertEquals(PermissionsData(fineLocation = true, coarseLocation = true, backgroundLocation = true, notifications = true), manager.currentPermissions())
    }

    @Test
    fun notifications_switched_off_in_settings() {
        grant(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).setNotificationsEnabled(false)
        assertEquals(false, manager.currentPermissions().notifications)
    }

    @Test
    @Config(sdk = [28])
    fun before_android_10_foreground_location_covers_the_background() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissions = manager.currentPermissions()
        assertEquals(true, permissions.backgroundLocation)
        assertEquals(true, permissions.notifications) // no runtime permission before Android 13
    }
}
