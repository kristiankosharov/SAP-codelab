package com.sap.codelab.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Replaces `Dispatchers.Main` (viewModelScope) with a test dispatcher. Pass [dispatcher] to `runTest`
 * so the ViewModel and the test share one scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) = Dispatchers.setMain(dispatcher)

    override fun afterEach(context: ExtensionContext) = Dispatchers.resetMain()
}
