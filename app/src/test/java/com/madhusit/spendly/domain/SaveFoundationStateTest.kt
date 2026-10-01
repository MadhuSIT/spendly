package com.madhusit.spendly.domain
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
class SaveFoundationStateTest {
    @Test fun save_delegatesToRepository() = runBlocking {
        val fake = object : FoundationRepository {
            var saved: String? = null
            override fun observe() = emptyFlow<FoundationState?>()
            override suspend fun save(message: String) { saved = message }
        }
        SaveFoundationState(fake)("hello")
        assertEquals("hello", fake.saved)
    }
}
