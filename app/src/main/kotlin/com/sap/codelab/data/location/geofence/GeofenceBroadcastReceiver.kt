package com.sap.codelab.data.location.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.sap.codelab.core.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "Geofence"

/**
 * Receives geofence transitions from Play services, also while the app is not running. Parses the
 * Play services event and hands the memo ids to [GeofenceTriggerHandler]; `goAsync` keeps the
 * process alive until the handler is done (the system allows about 10 s).
 */
@AndroidEntryPoint
internal class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: GeofenceTriggerHandler

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            // GEOFENCE_NOT_AVAILABLE: location was turned off and every geofence was removed;
            // the periodic proximity check registers them again.
            Timber.tag(TAG).w("Geofencing error: %s", GeofenceStatusCodes.getStatusCodeString(event.errorCode))
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            Timber.tag(TAG).d("Ignoring geofence transition %d", event.geofenceTransition)
            return
        }
        val memoIds = event.triggeringGeofences.orEmpty().mapNotNull { it.requestId.toLongOrNull() }
        Timber.tag(TAG).d("Entered geofences for memos %s", memoIds)
        if (memoIds.isEmpty()) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                handler.onGeofencesEntered(memoIds)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
