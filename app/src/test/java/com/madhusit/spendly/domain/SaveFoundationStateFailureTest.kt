package com.madhusit.spendly.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveFoundationStateFailureTest {
    @Test
    fun repositoryFailureIsPropagated() = runTest {
        val repository = object : FoundationRepository {
            override fun observe(): Flow<FoundationState?> = emptyFlow()
            override suspend fun save(message: String) {
                throw IllegalStateException("database unavailable")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            runBlocking { SaveFoundationState(repository)("test") }
        }
    }
}
