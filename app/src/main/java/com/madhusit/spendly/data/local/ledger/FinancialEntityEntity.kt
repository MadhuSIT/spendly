package com.madhusit.spendly.data.local.ledger

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.madhusit.spendly.domain.ledger.FinancialEntityType

@Entity(
    tableName = "financial_entities",
    indices = [
        androidx.room.Index(value = ["provider", "type", "lastFour"]),
        androidx.room.Index(value = ["active"])
    ]
)
data class FinancialEntityEntity(
    @PrimaryKey val id: String,
    val type: FinancialEntityType,
    val provider: String?,
    val name: String,
    val maskedIdentifier: String?,
    val lastFour: String?,
    val currency: String,
    val active: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)