package com.sap.codelab.background.di

import com.sap.codelab.background.BackgroundScheduler
import com.sap.codelab.background.BackgroundSchedulerImpl
import com.sap.codelab.background.GeofenceTriggerHandlerImpl
import com.sap.codelab.data.location.geofence.GeofenceTriggerHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface BackgroundModule {
    @Binds
    fun bindBackgroundScheduler(impl: BackgroundSchedulerImpl): BackgroundScheduler

    @Binds
    fun bindGeofenceTriggerHandler(impl: GeofenceTriggerHandlerImpl): GeofenceTriggerHandler
}
