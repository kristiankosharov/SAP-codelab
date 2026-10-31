package com.sap.codelab.data.location.source

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sap.codelab.data.location.GeofenceRegionData
import com.sap.codelab.data.location.geofence.GeofenceBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val TAG = "GeofenceSource"

/**
 * Part of the PendingIntent's identity. Picked once and fixed: Play services matches registered geofences
 * to the intent, so a value that changed per process would orphan the geofences of the previous run.
 */
private const val GEOFENCE_REQUEST_CODE = 48_213

/**
 * Play services geofencing. Transitions are delivered to [GeofenceBroadcastReceiver], so they arrive
 * even when the app is in the background or its process is dead.
 */
internal class GeofenceDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GeofenceDataSource {

    private val client = LocationServices.getGeofencingClient(context)

    // Mutable: Play services adds the geofencing event to the intent. Explicit component, so allowed on API 34+.
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    @SuppressLint("MissingPermission") // LocationTrackingRepositoryImpl checks the permission first
    override suspend fun replaceAll(regions: List<GeofenceRegionData>) {
        client.removeGeofences(pendingIntent).await()
        Timber.tag(TAG).d("Removed all geofences")
        if (regions.isEmpty()) return
        val request = GeofencingRequest.Builder()
            // Already inside when the memo is created or the device boots: remind right away.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(regions.map { it.toGeofence() })
            .build()
        client.addGeofences(request, pendingIntent).await()
        Timber.tag(TAG).d("Registered %d geofences for memos %s", regions.size, regions.map { it.id })
    }

    override suspend fun remove(ids: List<Long>) {
        client.removeGeofences(ids.map(Long::toString)).await()
        Timber.tag(TAG).d("Removed geofences for memos %s", ids)
    }

    private fun GeofenceRegionData.toGeofence(): Geofence = Geofence.Builder()
        .setRequestId(id.toString())
        .setCircularRegion(latitude, longitude, radiusMeters)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()
}
