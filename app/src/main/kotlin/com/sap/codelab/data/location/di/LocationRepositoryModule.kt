package com.sap.codelab.data.location.di

import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.location.LocationTrackingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface LocationRepositoryModule {
    @Binds
    fun bindLocationTrackingRepository(impl: LocationTrackingRepositoryImpl): LocationTrackingRepository
}
