package com.sap.codelab.data.location.geofence

/**
 * What happens when the device enters memo geofences. Declared here, implemented by the background
 * layer (`background/`), so the data layer never depends on domain use cases.
 */
fun interface GeofenceTriggerHandler {
    /** [memoIds] are the request ids of the geofences that were entered. */
    suspend fun onGeofencesEntered(memoIds: List<Long>)
}
