package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.domain.model.Memo
import javax.inject.Inject

/**
 * Shared by the two reminder paths (geofence, periodic check): notifies, then marks the memos as
 * reminded and drops their geofences. A memo whose notification could not be shown (notifications
 * blocked) stays active, so it reminds once they are allowed again.
 */
internal class MemoReminder @Inject constructor(
    private val memoRepository: MemoRepository,
    private val locationRepository: LocationTrackingRepository,
    private val notifier: ReminderNotifier,
) {
    suspend fun remind(memos: List<Memo>): AppResult<Int> {
        val reminded = memos
            .filter { it.isActive }
            .filter { notifier.showReminder(it.id, it.title, it.reminderText) is AppResult.Success }
            .map { it.id }
        if (reminded.isEmpty()) return AppResult.Success(0)
        val marked = memoRepository.markReminded(reminded)
        if (marked is AppResult.Failure) return marked
        locationRepository.removeGeofences(reminded)
        return AppResult.Success(reminded.size)
    }
}
