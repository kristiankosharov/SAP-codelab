package com.sap.codelab.data.notification.di

import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.data.notification.ReminderNotifierImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationModule {
    @Binds
    fun bindReminderNotifier(impl: ReminderNotifierImpl): ReminderNotifier
}
