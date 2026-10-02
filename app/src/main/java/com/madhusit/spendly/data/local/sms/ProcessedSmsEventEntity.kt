package com.madhusit.spendly.data.local.sms

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processed_sms_events",
    indices = [Index(value = ["createdAtEpochMillis"])]
)
data class ProcessedSmsEventEntity(
    @PrimaryKey val fingerprint: String,
    val transactionId: String?,
    val status: String,
    val createdAtEpochMillis: Long
)
