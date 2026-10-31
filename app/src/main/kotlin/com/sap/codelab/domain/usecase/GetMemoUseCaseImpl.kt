package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.core.map
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.Memo
import javax.inject.Inject

internal class GetMemoUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
) : GetMemoUseCase {

    override suspend fun invoke(id: Long): AppResult<Memo> = memoRepository.getMemo(id).map { it.toDomain() }
}
