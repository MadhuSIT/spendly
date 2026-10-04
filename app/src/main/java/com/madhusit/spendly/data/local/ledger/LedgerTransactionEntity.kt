package com.madhusit.spendly.data.local.ledger

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.madhusit.spendly.domain.ledger.TransactionCategory
import com.madhusit.spendly.domain.ledger.TransactionStatus
import com.madhusit.spendly.domain.ledger.TransactionType

@Entity(
    tableName = "ledger_transactions",
    foreignKeys = [
        ForeignKey(
            entity = FinancialEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceEntityId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = FinancialEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationEntityId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sourceEntityId", "transactionTimestamp"]),
        Index(value = ["destinationEntityId", "transactionTimestamp"]),
        Index(value = ["type", "status", "transactionTimestamp"]),
        Index(value = ["referenceNumber"]),
        Index(value = ["upiReference"])
    ]
)
data class LedgerTransactionEntity(
    @PrimaryKey val id: String,
    val sourceEntityId: String,
    val destinationEntityId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val merchantName: String?,
    val description: String?,
    val transactionTimestamp: Long,
    val status: TransactionStatus,
    val referenceNumber: String?,
    val upiReference: String?,
    val rawEventReference: String?,
    val parserSource: String?,
    val parserVersion: String?,
    val confidence: Double?,
    val reviewRequired: Boolean,
    val category: TransactionCategory?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)