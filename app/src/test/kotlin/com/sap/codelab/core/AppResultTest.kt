package com.sap.codelab.core

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppResultTest {

    @Test
    fun `the coroutines module provides IO and a process scope`() {
        val io = com.sap.codelab.core.di.CoroutinesModule.provideIoDispatcher()
        assertEquals(kotlinx.coroutines.Dispatchers.IO, io)
        com.sap.codelab.core.di.CoroutinesModule.provideApplicationScope(io).coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private val failure = AppResult.Failure(AppError.Unknown(null))

    @Test
    fun `map transforms success and keeps failure`() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })
        assertEquals(failure, failure.map { _: Int -> 0 })
    }

    @Test
    fun `onSuccess and onFailure run only for their case`() {
        var seen: Int? = null
        var error: AppError? = null
        AppResult.Success(1).onSuccess { seen = it }.onFailure { error = it }
        assertEquals(1, seen)
        assertNull(error)

        seen = null
        failure.onSuccess { seen = 0 }.onFailure { error = it }
        assertNull(seen)
        assertEquals(failure.error, error)
    }

    @Test
    fun `mapResult maps every success in a flow`() = runTest {
        flowOf(AppResult.Success(1), failure).mapResult { it + 1 }.test {
            assertEquals(AppResult.Success(2), awaitItem())
            assertEquals(failure, awaitItem())
            awaitComplete()
        }
    }
}
