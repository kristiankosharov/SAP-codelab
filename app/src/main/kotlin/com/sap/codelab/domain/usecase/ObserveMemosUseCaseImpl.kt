package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.core.mapResult
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.Memo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class ObserveMemosUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
) : ObserveMemosUseCase {

    override fun invoke(includeDone: Boolean): Flow<AppResult<List<Memo>>> =
        memoRepository.observeMemos(includeDone).mapResult { memos -> memos.map { it.toDomain() } }
}
