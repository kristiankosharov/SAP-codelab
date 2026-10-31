package com.sap.codelab.data.memo.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sap.codelab.core.IoDispatcher
import com.sap.codelab.data.memo.local.MemoDao
import com.sap.codelab.data.memo.local.MemoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

private const val DATABASE_NAME = "memos.db"

@Module
@InstallIn(SingletonComponent::class)
internal object MemoDatabaseModule {
    @Provides
    @Singleton
    fun provideMemoDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MemoDatabase = Room.databaseBuilder(context, MemoDatabase::class.java, DATABASE_NAME)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .build()

    @Provides
    fun provideMemoDao(database: MemoDatabase): MemoDao = database.memoDao()
}
