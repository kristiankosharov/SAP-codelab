package com.sap.codelab.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.sap.codelab.App
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The real Application with the real Hilt graph: background work is scheduled on start. */
@RunWith(AndroidJUnit4::class)
@Config(application = App::class)
class AppTest {

    @Test
    fun schedules_background_work_on_start() {
        val app = ApplicationProvider.getApplicationContext<App>()
        val workManager = WorkManager.getInstance(app)
        assertEquals(1, workManager.getWorkInfosForUniqueWork("proximity_check").get().size)
        assertEquals(1, workManager.getWorkInfosForUniqueWork("geofence_sync").get().size)
    }
}
