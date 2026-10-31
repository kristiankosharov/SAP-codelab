package com.sap.codelab.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.sap.codelab.background.BackgroundScheduler
import com.sap.codelab.background.RestartReceiver
import com.sap.codelab.background.di.BackgroundModule
import com.sap.codelab.core.ApplicationScope
import com.sap.codelab.core.IoDispatcher
import com.sap.codelab.core.di.CoroutinesModule
import com.sap.codelab.data.location.geofence.GeofenceBroadcastReceiver
import com.sap.codelab.data.location.geofence.GeofenceTriggerHandler
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@HiltAndroidTest
@UninstallModules(BackgroundModule::class, CoroutinesModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ReceiversTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @BindValue
    val handler: GeofenceTriggerHandler = mockk(relaxed = true)

    @BindValue
    val scheduler: BackgroundScheduler = mockk(relaxed = true)

    @BindValue
    @ApplicationScope
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    @BindValue
    @IoDispatcher
    val io: CoroutineDispatcher = Dispatchers.Unconfined

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val event = mockk<GeofencingEvent>()

    @Before
    fun setUp() {
        hilt.inject()
        mockkStatic(GeofencingEvent::class)
        every { GeofencingEvent.fromIntent(any()) } returns event
        every { event.hasError() } returns false
    }

    @After
    fun tearDown() = unmockkAll()

    private fun broadcast(
        receiver: Class<*>,
        action: String? = null,
    ) {
        context.sendBroadcast(Intent(action).setComponent(ComponentName(context, receiver)))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun geofence(id: String) = mockk<Geofence> { every { requestId } returns id }

    @Test
    fun entering_geofences_hands_the_memo_ids_over() {
        every { event.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { event.triggeringGeofences } returns listOf(geofence("1"), geofence("2"), geofence("not-a-memo"))

        broadcast(GeofenceBroadcastReceiver::class.java)

        coVerify { handler.onGeofencesEntered(listOf(1L, 2L)) }
    }

    @Test
    fun other_transitions_errors_and_empty_events_are_ignored() {
        every { event.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_EXIT
        broadcast(GeofenceBroadcastReceiver::class.java)

        every { event.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { event.triggeringGeofences } returns emptyList()
        broadcast(GeofenceBroadcastReceiver::class.java)

        every { event.hasError() } returns true
        every { event.errorCode } returns 1000
        broadcast(GeofenceBroadcastReceiver::class.java)

        every { GeofencingEvent.fromIntent(any()) } returns null
        broadcast(GeofenceBroadcastReceiver::class.java)

        coVerify(exactly = 0) { handler.onGeofencesEntered(any()) }
    }

    @Test
    fun boot_and_app_update_reregister_geofences() {
        broadcast(RestartReceiver::class.java, Intent.ACTION_BOOT_COMPLETED)
        broadcast(RestartReceiver::class.java, Intent.ACTION_MY_PACKAGE_REPLACED)
        broadcast(RestartReceiver::class.java, "some.other.ACTION")
        verify(exactly = 2) { scheduler.scheduleLocationTracking() }
    }
}
