package com.sap.codelab.data.location

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

private const val TAG = "Location"

private val GEOFENCE_STATUS_CODES = setOf(
    GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE,
    GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES,
    GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS,
    GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION,
    GeofenceStatusCodes.GEOFENCE_REQUEST_TOO_FREQUENT,
)

/** Runs a location/geofencing call. Every failure becomes an [AppError.Location]; cancellation is rethrown. */
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
internal suspend inline fun <T> safeLocationCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toLocationError())
    }

/** Maps a failed call to an [AppError.Location] and logs it (the wrappers are the only place errors are logged). */
internal fun Exception.toLocationError(): AppError.Location {
    val error = when {
        this is SecurityException -> AppError.Location.PermissionDenied(this)
        this is ApiException && statusCode in GEOFENCE_STATUS_CODES -> AppError.Location.Geofence(statusCode, this)
        this is ApiException -> AppError.Location.Unavailable(this)
        else -> AppError.Location.Unknown(this)
    }
    if (error is AppError.Location.Unknown) {
        Timber.tag(TAG).e(this, "Unexpected location error")
    } else {
        Timber.tag(TAG).w(this, "Location call failed: %s", error::class.simpleName)
    }
    return error
}
