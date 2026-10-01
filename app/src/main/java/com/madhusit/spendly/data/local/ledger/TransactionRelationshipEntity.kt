package com.madhusit.spendly.data.local.ledger

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_relationships",
    foreignKeys = [
        ForeignKey(
            entity = LedgerTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromTransactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LedgerTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["toTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fromTransactionId", "relationshipType"]),
        Index(value = ["toTransactionId", "relationshipType"]),
        Index(value = ["fromTransactionId", "toTransactionId", "relationshipType"], unique = true)
    ]
)
data class TransactionRelationshipEntity(
    @PrimaryKey val id: String,
    val fromTransactionId: String,
    val toTransactionId: String,
    val relationshipType: String,
    val createdAtEpochMillis: Long
)