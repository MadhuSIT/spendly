package com.madhusit.spendly.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionRelationshipDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(relationship: TransactionRelationshipEntity)

    @Query("SELECT * FROM transaction_relationships WHERE fromTransactionId = :transactionId OR toTransactionId = :transactionId")
    suspend fun findForTransaction(transactionId: String): List<TransactionRelationshipEntity>

    @Query("SELECT * FROM transaction_relationships")
    suspend fun findAll(): List<TransactionRelationshipEntity>
}