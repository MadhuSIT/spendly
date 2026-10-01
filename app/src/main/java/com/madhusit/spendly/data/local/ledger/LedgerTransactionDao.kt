package com.madhusit.spendly.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerTransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: LedgerTransactionEntity)

    @Update
    suspend fun update(transaction: LedgerTransactionEntity)

    @Query("SELECT * FROM ledger_transactions ORDER BY transactionTimestamp DESC")
    fun observeAll(): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions")
    suspend fun findAll(): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE id = :id")
    suspend fun findById(id: String): LedgerTransactionEntity?

    @Query("SELECT * FROM ledger_transactions WHERE referenceNumber = :reference OR upiReference = :reference LIMIT 1")
    suspend fun findByReference(reference: String): LedgerTransactionEntity?
}