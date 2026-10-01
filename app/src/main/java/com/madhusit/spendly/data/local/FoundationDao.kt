package com.madhusit.spendly.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoundationDao {
    @Query("SELECT * FROM foundation_state WHERE id = 1")
    fun observe(): Flow<FoundationEntity?>

    @Upsert
    suspend fun save(entity: FoundationEntity)
}
