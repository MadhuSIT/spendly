package com.madhusit.spendly.data.local.sms

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProcessedSmsEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun claim(event: ProcessedSmsEventEntity): Long

    @Query("SELECT * FROM processed_sms_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun find(fingerprint: String): ProcessedSmsEventEntity?

    @Query(
        "UPDATE processed_sms_events " +
            "SET transactionId = :transactionId, status = :status " +
            "WHERE fingerprint = :fingerprint"
    )
    suspend fun updateResult(
        fingerprint: String,
        transactionId: String?,
        status: String
    )

    @Query("DELETE FROM processed_sms_events WHERE transactionId IN (:transactionIds)")
    suspend fun deleteByTransactionIds(transactionIds: List<String>)

    @Query("DELETE FROM processed_sms_events")
    suspend fun deleteAll()
}
