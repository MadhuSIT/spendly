package com.madhusit.spendly.domain

import kotlinx.coroutines.flow.Flow

data class FoundationState(val message: String)

interface FoundationRepository {
    fun observe(): Flow<FoundationState?>
    suspend fun save(message: String)
}
