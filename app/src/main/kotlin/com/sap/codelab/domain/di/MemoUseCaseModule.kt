package com.sap.codelab.domain.di

import com.sap.codelab.core.mapResult
import com.sap.codelab.core.map
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.usecase.GetMemoUseCase
import com.sap.codelab.domain.usecase.ObserveMemosUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** One-line memo use cases, provided as lambdas. Unscoped: every caller gets its own instance. */
@Module
@InstallIn(SingletonComponent::class)
internal object MemoUseCaseModule {
    @Provides
    fun provideObserveMemosUseCase(repository: MemoRepository) = ObserveMemosUseCase { includeDone ->
        repository.observeMemos(includeDone).mapResult { memos -> memos.map { it.toDomain() } }
    }

    @Provides
    fun provideGetMemoUseCase(repository: MemoRepository) = GetMemoUseCase { id ->
        repository.getMemo(id).map { it.toDomain() }
    }
}
