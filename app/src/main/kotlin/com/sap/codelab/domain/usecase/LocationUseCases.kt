package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.PermissionStatus

fun interface GetCurrentLocationUseCase {
    suspend operator fun invoke(): AppResult<GeoPoint>
}

fun interface GetPermissionStatusUseCase {
    operator fun invoke(): PermissionStatus
}

/**
 * Registers one geofence per active memo (the [com.sap.codelab.domain.model.MAX_GEOFENCES] nearest
 * when there are more), replacing whatever was registered before.
 */
interface SyncGeofencesUseCase {
    suspend operator fun invoke(): AppResult<Unit>
}

/** Fallback for geofencing: reminds of every active memo within the reminder radius of the current location. Returns how many. */
interface NotifyNearbyMemosUseCase {
    suspend operator fun invoke(): AppResult<Int>
}

/** Reminds of the memos whose geofences were entered (skipping done or already reminded ones). Returns how many. */
interface NotifyTriggeredMemosUseCase {
    suspend operator fun invoke(memoIds: List<Long>): AppResult<Int>
}
