package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toDomain
import javax.inject.Inject

internal class NotifyTriggeredMemosUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
    private val reminder: MemoReminder,
) : NotifyTriggeredMemosUseCase {

    override suspend fun invoke(memoIds: List<Long>): AppResult<Int> =
        when (val result = memoRepository.getMemos(memoIds)) {
            is AppResult.Success -> reminder.remind(result.data.map { it.toDomain() })
            is AppResult.Failure -> result
        }
}
