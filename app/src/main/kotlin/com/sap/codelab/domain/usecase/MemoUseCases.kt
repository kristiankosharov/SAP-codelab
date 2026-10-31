package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.Memo
import kotlinx.coroutines.flow.Flow

/** All memos, or only the open ones, newest first. */
fun interface ObserveMemosUseCase {
    operator fun invoke(includeDone: Boolean): Flow<AppResult<List<Memo>>>
}

fun interface GetMemoUseCase {
    suspend operator fun invoke(id: Long): AppResult<Memo>
}

/** Saves a new memo and starts watching its location. Returns the new memo's id. */
interface SaveMemoUseCase {
    suspend operator fun invoke(
        title: String,
        description: String,
        location: GeoPoint,
    ): AppResult<Long>
}

/** Marks a memo as done and stops watching its location. */
interface MarkMemoDoneUseCase {
    suspend operator fun invoke(id: Long): AppResult<Unit>
}
