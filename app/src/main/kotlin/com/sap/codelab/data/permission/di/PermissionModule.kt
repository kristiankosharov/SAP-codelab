package com.sap.codelab.data.permission.di

import com.sap.codelab.data.permission.PermissionManager
import com.sap.codelab.data.permission.PermissionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface PermissionModule {
    @Binds
    fun bindPermissionManager(impl: PermissionManagerImpl): PermissionManager
}
