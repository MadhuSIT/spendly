package com.madhusit.spendly.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialEntityDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FinancialEntityEntity)

    @Query("SELECT * FROM financial_entities ORDER BY name")
    fun observeAll(): Flow<List<FinancialEntityEntity>>

    @Query("SELECT * FROM financial_entities WHERE id = :id")
    suspend fun findById(id: String): FinancialEntityEntity?

    @Query("DELETE FROM financial_entities")
    suspend fun deleteAll()
}