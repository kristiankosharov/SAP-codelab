package com.sap.codelab.data.memo.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * The app's only database. Bump [version] with every schema change and add an AutoMigration (or a
 * Migration_<from>_<to>); schemas are exported to app/schemas.
 */
@Database(entities = [MemoEntity::class], version = 1)
internal abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}
