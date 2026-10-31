package com.sap.codelab.domain.model

data class Memo(
    val id: Long,
    val title: String,
    val description: String,
    val location: GeoPoint,
    val isDone: Boolean,
    /** True once the reminder was shown; a memo reminds only once. */
    val isReminded: Boolean,
) {
    /** Open and not reminded yet: location tracking watches it. */
    val isActive: Boolean get() = !isDone && !isReminded

    /** The notification text: the first [REMINDER_TEXT_LENGTH] characters of the description. */
    val reminderText: String get() = description.take(REMINDER_TEXT_LENGTH)
}
