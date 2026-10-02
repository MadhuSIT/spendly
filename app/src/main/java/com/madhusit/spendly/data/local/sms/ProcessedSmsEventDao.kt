package com.madhusit.spendly.data.local.sms

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProcessedSmsEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: ProcessedSmsEventEntity): Long

    @Query("SELECT * FROM processed_sms_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun find(fingerprint: String): ProcessedSmsEventEntity?
}
