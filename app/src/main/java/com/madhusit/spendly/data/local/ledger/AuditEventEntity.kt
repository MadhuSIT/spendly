package com.madhusit.spendly.data.local.ledger

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_events",
    indices = [Index(value = ["entityType", "entityId", "createdAtEpochMillis"])]
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val actor: String,
    val details: String?,
    val createdAtEpochMillis: Long
)