package com.madhusit.spendly.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AuditEventDao {
    @Insert
    suspend fun insert(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events WHERE entityId = :entityId ORDER BY createdAtEpochMillis DESC")
    suspend fun findForEntity(entityId: String): List<AuditEventEntity>
}