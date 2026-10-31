package com.sap.codelab.data.memo

/** A memo as the data layer exposes it. Plain and immutable: no Room annotations. */
data class MemoData(
    val id: Long,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val isDone: Boolean,
    /** True once the reminder notification was shown; a memo reminds only once. */
    val isReminded: Boolean,
)
