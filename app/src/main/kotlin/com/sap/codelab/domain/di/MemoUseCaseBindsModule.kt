package com.sap.codelab.domain.di

import com.sap.codelab.domain.usecase.MarkMemoDoneUseCase
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCaseImpl
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
    fun bindMarkMemoDoneUseCase(impl: MarkMemoDoneUseCaseImpl): MarkMemoDoneUseCase
}
