package com.madhusit.spendly.data.repository

import com.madhusit.spendly.data.local.FoundationDao
import com.madhusit.spendly.data.local.FoundationEntity
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.FoundationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoundationRepositoryImpl(private val dao: FoundationDao) : FoundationRepository {
    override fun observe(): Flow<FoundationState?> =
        dao.observe().map { it?.let { entity -> FoundationState(entity.message) } }

    override suspend fun save(message: String) {
        dao.save(FoundationEntity(message = message))
    }
}
