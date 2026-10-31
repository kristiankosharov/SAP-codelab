package com.sap.codelab.domain.di

import com.sap.codelab.domain.usecase.GetMemoUseCase
import com.sap.codelab.domain.usecase.GetMemoUseCaseImpl
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCase
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCaseImpl
import com.sap.codelab.domain.usecase.ObserveMemosUseCase
import com.sap.codelab.domain.usecase.ObserveMemosUseCaseImpl
import com.sap.codelab.domain.usecase.SaveMemoUseCase
import com.sap.codelab.domain.usecase.SaveMemoUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MemoUseCaseBindsModule {
    @Binds
    fun bindSaveMemoUseCase(impl: SaveMemoUseCaseImpl): SaveMemoUseCase

    @Binds
    fun bindObserveMemosUseCase(impl: ObserveMemosUseCaseImpl): ObserveMemosUseCase

    @Binds
    fun bindGetMemoUseCase(impl: GetMemoUseCaseImpl): GetMemoUseCase

    @Binds
    fun bindMarkMemoDoneUseCase(impl: MarkMemoDoneUseCaseImpl): MarkMemoDoneUseCase
}
