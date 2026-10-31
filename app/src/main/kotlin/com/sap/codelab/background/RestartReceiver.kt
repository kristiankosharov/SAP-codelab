package com.sap.codelab.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

private const val TAG = "Restart"

/** Geofences don't survive a reboot or an app update; this registers them again. */
@AndroidEntryPoint
internal class RestartReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: BackgroundScheduler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.tag(TAG).d("%s: re-registering geofences", intent.action)
                scheduler.scheduleLocationTracking()
            }
        }
    }
}
