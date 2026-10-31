package com.sap.codelab.data

import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.memo.local.asDbResult
import com.sap.codelab.data.memo.local.safeDbCall
import com.sap.codelab.data.memo.local.toLocalError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SafeDbCallTest {

    @Test
    fun `success is wrapped`() = runTest {
        assertEquals(AppResult.Success(1), safeDbCall { 1 })
    }

    @Test
    fun `constraint violations become Constraint`() {
        assertInstanceOf(AppError.Local.Constraint::class.java, SQLiteConstraintException("unique").toLocalError())
        // The bundled driver's "Error code: 2067" message case runs in BundledDriverErrorTest (Robolectric):
        // android.jar stubs drop exception messages on the plain JVM.
    }

    @Test
    fun `other SQL errors become Database and anything else Unknown`() {
        assertInstanceOf(AppError.Local.Database::class.java, SQLException("Error code: 10, message: disk I/O").toLocalError())
        assertInstanceOf(AppError.Local.Database::class.java, SQLException("no code").toLocalError())
        assertInstanceOf(AppError.Local.Unknown::class.java, IllegalStateException("bug").toLocalError())
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        assertThrows<CancellationException> { safeDbCall { throw CancellationException("stop") } }
    }

    @Test
    fun `asDbResult wraps observed values`() = runTest {
        flowOf(1).asDbResult().test {
            assertEquals(AppResult.Success(1), awaitItem())
            awaitComplete()
        }
    }
}
