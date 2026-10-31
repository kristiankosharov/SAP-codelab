package com.sap.codelab.core

import javax.inject.Qualifier

/** The IO dispatcher. Inject it; never use `Dispatchers.IO` directly. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** A process-lifetime scope (`SupervisorJob() + IO`), e.g. for background work started by receivers. Never a shorter scope. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
