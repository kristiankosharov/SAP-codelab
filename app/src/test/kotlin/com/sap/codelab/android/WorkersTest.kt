package com.sap.codelab.android

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.sap.codelab.background.BackgroundSchedulerImpl
import com.sap.codelab.background.GeofenceSyncWorker
import com.sap.codelab.background.ProximityCheckWorker
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.usecase.NotifyNearbyMemosUseCase
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkersTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notifyNearby = mockk<NotifyNearbyMemosUseCase>()
    private val syncGeofences = mockk<SyncGeofencesUseCase>()

    private val factory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? =
            when (workerClassName) {
                ProximityCheckWorker::class.java.name -> ProximityCheckWorker(appContext, workerParameters, notifyNearby, syncGeofences)
                GeofenceSyncWorker::class.java.name -> GeofenceSyncWorker(appContext, workerParameters, syncGeofences)
                else -> null
            }
    }

    private inline fun <reified W : ListenableWorker> worker() = TestListenableWorkerBuilder<W>(app).setWorkerFactory(factory).build()

    @Test
    fun proximity_check_reminds_and_resyncs() = runBlocking {
        coEvery { notifyNearby() } returns AppResult.Success(1)
        coEvery { syncGeofences() } returns AppResult.Success(Unit)
        assertEquals(ListenableWorker.Result.success(), worker<ProximityCheckWorker>().doWork())
        coVerify { syncGeofences() }
    }

    @Test
    fun proximity_check_retries_database_failures_only() = runBlocking {
        coEvery { syncGeofences() } returns AppResult.Success(Unit)
        coEvery { notifyNearby() } returns AppResult.Failure(AppError.Local.Database(null))
        assertEquals(ListenableWorker.Result.retry(), worker<ProximityCheckWorker>().doWork())
        coEvery { notifyNearby() } returns AppResult.Failure(AppError.Location.Unavailable(null))
        assertEquals(ListenableWorker.Result.success(), worker<ProximityCheckWorker>().doWork())
    }

    @Test
    fun geofence_sync() = runBlocking {
        coEvery { syncGeofences() } returns AppResult.Success(Unit)
        assertEquals(ListenableWorker.Result.success(), worker<GeofenceSyncWorker>().doWork())
        coEvery { syncGeofences() } returns AppResult.Failure(AppError.Location.PermissionDenied(null))
        assertEquals(ListenableWorker.Result.success(), worker<GeofenceSyncWorker>().doWork())
    }

    @Test
    fun scheduler_enqueues_unique_work() {
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        val scheduler = BackgroundSchedulerImpl(app)
        scheduler.scheduleLocationTracking()
        scheduler.scheduleLocationTracking() // KEEP: still one of each

        val workManager = WorkManager.getInstance(app)
        assertEquals(1, workManager.getWorkInfosForUniqueWork("proximity_check").get().size)
        assertEquals(WorkInfo.State.ENQUEUED, workManager.getWorkInfosForUniqueWork("proximity_check").get().single().state)
        assertEquals(1, workManager.getWorkInfosForUniqueWork("geofence_sync").get().size)
    }
}
