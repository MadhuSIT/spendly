package com.madhusit.spendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FoundationEntity::class], version = 1, exportSchema = true)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
}
