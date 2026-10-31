package com.sap.codelab.core

import app.cash.turbine.test
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class RetryingResultTest {

    private val mapError: (Exception) -> AppError = { AppError.Local.Database(it) }

    @Test
    fun `emits a failure, re-subscribes and recovers`() = runTest {
        var subscriptions = 0
        val source = flow {
            subscriptions++
            if (subscriptions == 1) throw IOException("boom")
            emit("ok")
        }
        source.asRetryingResult(mapError).test {
            assertInstanceOf(AppResult.Failure::class.java, awaitItem())
            assertEquals(AppResult.Success("ok"), awaitItem())
            awaitComplete()
        }
        assertEquals(2, subscriptions)
    }

    @Test
    fun `ends after maxRetries consecutive failures`() = runTest {
        var subscriptions = 0
        val source = flow<String> {
            subscriptions++
            throw IOException("always")
        }
        source.asRetryingResult(mapError, maxRetries = 2).test {
            repeat(3) { assertInstanceOf(AppResult.Failure::class.java, awaitItem()) }
            awaitComplete()
        }
        assertEquals(3, subscriptions)
    }

    @Test
    fun `a success resets the failure counter`() = runTest {
        var subscriptions = 0
        // fail, succeed-then-fail, fail, succeed: never two failures in a row without a success
        val source = flow {
            subscriptions++
            when (subscriptions) {
                1, 3 -> throw IOException("fail")
                2 -> {
                    emit(2)
                    throw IOException("fail after success")
                }
                else -> emit(subscriptions)
            }
        }
        source.asRetryingResult(mapError, maxRetries = 2).test {
            assertInstanceOf(AppResult.Failure::class.java, awaitItem())
            assertEquals(AppResult.Success(2), awaitItem())
            assertInstanceOf(AppResult.Failure::class.java, awaitItem())
            assertInstanceOf(AppResult.Failure::class.java, awaitItem())
            assertEquals(AppResult.Success(4), awaitItem())
            awaitComplete()
        }
    }
}
