package com.sap.codelab.data.memo.di

import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.data.memo.MemoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MemoRepositoryModule {
    @Binds
    fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository
}
