package com.madhusit.spendly.data.local.sms

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_sms_events")
data class ProcessedSmsEventEntity(
    @PrimaryKey val fingerprint: String,
    val transactionId: String?,
    val status: String,
    val createdAtEpochMillis: Long
)
