package com.sap.codelab.data.location.di

import com.sap.codelab.data.location.source.GeofenceDataSource
import com.sap.codelab.data.location.source.GeofenceDataSourceImpl
import com.sap.codelab.data.location.source.LocationDataSource
import com.sap.codelab.data.location.source.LocationDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface LocationDataSourceModule {
    @Binds
    fun bindLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource

    @Binds
    fun bindGeofenceDataSource(impl: GeofenceDataSourceImpl): GeofenceDataSource
}
