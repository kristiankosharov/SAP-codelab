package com.sap.codelab.domain.di

import com.sap.codelab.core.map
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.permission.PermissionManager
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.usecase.GetCurrentLocationUseCase
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** One-line location use cases, provided as lambdas. */
@Module
@InstallIn(SingletonComponent::class)
internal object LocationUseCaseModule {
    @Provides
    fun provideGetCurrentLocationUseCase(repository: LocationTrackingRepository) = GetCurrentLocationUseCase {
        repository.getCurrentLocation().map { it.toDomain() }
    }

    @Provides
    fun provideGetPermissionStatusUseCase(permissionManager: PermissionManager) = GetPermissionStatusUseCase {
        permissionManager.currentPermissions().toDomain()
    }
}
