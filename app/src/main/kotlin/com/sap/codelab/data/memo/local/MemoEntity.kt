package com.sap.codelab.data.memo.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** The index serves the location queries: filter on the two flags, then range-scan on latitude. */
@Entity(tableName = "memos", indices = [Index(value = ["isDone", "isReminded", "latitude"])])
internal data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val isDone: Boolean,
    val isReminded: Boolean,
)
