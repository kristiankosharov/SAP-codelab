package com.sap.codelab.android

import android.app.Application
import android.database.SQLException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.core.AppError
import com.sap.codelab.data.memo.di.MemoDatabaseModule
import com.sap.codelab.data.memo.local.toLocalError
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledDriverErrorTest {

    @Test
    fun driver_constraint_codes_in_the_message_are_constraints() {
        // 2067 = SQLITE_CONSTRAINT_UNIQUE; its primary code (low byte) is SQLITE_CONSTRAINT (19)
        assertTrue(SQLException("Error code: 2067, message: UNIQUE constraint failed").toLocalError() is AppError.Local.Constraint)
        assertTrue(SQLException("Error code: 10, message: disk I/O error").toLocalError() is AppError.Local.Database)
    }

    @Test
    fun the_database_module_builds_the_database_and_dao() {
        // Building is lazy: no query runs, so the bundled native driver is not loaded on the JVM.
        val database = MemoDatabaseModule.provideMemoDatabase(ApplicationProvider.getApplicationContext<Application>(), Dispatchers.Unconfined)
        MemoDatabaseModule.provideMemoDao(database)
        database.close()
    }
}
