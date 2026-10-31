package com.sap.codelab.domain.model

/** "Reaching the location" means being within this distance of the memo's point. */
const val REMINDER_RADIUS_METERS = 200f

/** How much of the memo text a reminder notification shows. */
const val REMINDER_TEXT_LENGTH = 140

/** Play services allows at most 100 active geofences per app; the nearest memos get them. */
const val MAX_GEOFENCES = 100
