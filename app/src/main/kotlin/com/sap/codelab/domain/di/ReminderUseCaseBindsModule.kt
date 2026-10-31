package com.sap.codelab.domain.di

import com.sap.codelab.domain.usecase.GetCurrentLocationUseCase
import com.sap.codelab.domain.usecase.GetCurrentLocationUseCaseImpl
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCaseImpl
import com.sap.codelab.domain.usecase.NotifyNearbyMemosUseCase
import com.sap.codelab.domain.usecase.NotifyNearbyMemosUseCaseImpl
import com.sap.codelab.domain.usecase.NotifyTriggeredMemosUseCase
import com.sap.codelab.domain.usecase.NotifyTriggeredMemosUseCaseImpl
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import com.sap.codelab.domain.usecase.SyncGeofencesUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Location tracking and reminder use cases. */
@Module
@InstallIn(SingletonComponent::class)
internal interface ReminderUseCaseBindsModule {
    @Binds
    fun bindGetCurrentLocationUseCase(impl: GetCurrentLocationUseCaseImpl): GetCurrentLocationUseCase

    @Binds
    fun bindGetPermissionStatusUseCase(impl: GetPermissionStatusUseCaseImpl): GetPermissionStatusUseCase

    @Binds
    fun bindSyncGeofencesUseCase(impl: SyncGeofencesUseCaseImpl): SyncGeofencesUseCase

    @Binds
    fun bindNotifyNearbyMemosUseCase(impl: NotifyNearbyMemosUseCaseImpl): NotifyNearbyMemosUseCase

    @Binds
    fun bindNotifyTriggeredMemosUseCase(impl: NotifyTriggeredMemosUseCaseImpl): NotifyTriggeredMemosUseCase
}
