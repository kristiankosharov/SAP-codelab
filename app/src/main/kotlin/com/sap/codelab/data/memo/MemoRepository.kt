package com.sap.codelab.data.memo

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import kotlinx.coroutines.flow.Flow

/** Memo CRUD. Room is the single source of truth; every call returns an [AppResult] and never throws. */
interface MemoRepository {
    /** All memos, or only those not marked as done, newest first. */
    fun observeMemos(includeDone: Boolean): Flow<AppResult<List<MemoData>>>

    suspend fun getMemo(id: Long): AppResult<MemoData>

    /** Open memos whose reminder has not been shown yet: the ones location tracking watches. */
    suspend fun getActiveMemos(): AppResult<List<MemoData>>

    suspend fun countActiveMemos(): AppResult<Int>

    /** Active memos that may be within [radiusMeters] of [center]: a bounding box, so callers still check the exact distance. */
    suspend fun getActiveMemosNear(
        center: GeoPointData,
        radiusMeters: Double,
    ): AppResult<List<MemoData>>

    /** The [limit] active memos closest to [center], nearest first (approximate order, good enough to pick what to watch). */
    suspend fun getNearestActiveMemos(
        center: GeoPointData,
        limit: Int,
    ): AppResult<List<MemoData>>

    suspend fun getMemos(ids: List<Long>): AppResult<List<MemoData>>

    /** Inserts a new memo and returns its id. */
    suspend fun addMemo(memo: MemoData): AppResult<Long>

    suspend fun markDone(id: Long): AppResult<Unit>

    suspend fun markReminded(ids: List<Long>): AppResult<Unit>
}
