package com.sap.codelab.data.location.source

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sap.codelab.data.location.GeoPointData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val TAG = "LocationSource"
private val MAX_LOCATION_AGE = 2.minutes

/** Fused Location Provider (Play services). */
internal class LocationDataSourceImpl @Inject constructor(
    @ApplicationContext context: Context,
) : LocationDataSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    // await(CancellationTokenSource) is the only await that also cancels the location request.
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission") // LocationTrackingRepositoryImpl checks the permission first
    override suspend fun currentLocation(): GeoPointData? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE.inWholeMilliseconds)
            .build()
        val cancellation = CancellationTokenSource()
        Timber.tag(TAG).d("Requesting current location (balanced power, max age %d s)", MAX_LOCATION_AGE.inWholeSeconds)
        val current = client.getCurrentLocation(request, cancellation.token).await(cancellation)
        if (current != null) {
            current.logFix("Current location")
            return current.toData()
        }
        val last = client.lastLocation.await()
        if (last != null) last.logFix("No current fix; using last known location") else Timber.tag(TAG).d("No location available")
        return last?.toData()
    }

    /** Never the coordinates (personal data): only how old and how precise the fix is. */
    private fun Location.logFix(message: String) {
        val ageSeconds = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos).nanoseconds.inWholeSeconds
        Timber.tag(TAG).d("%s: provider %s, age %d s, accuracy %.0f m", message, provider, ageSeconds, accuracy)
    }

    private fun Location.toData() = GeoPointData(latitude, longitude)
}
