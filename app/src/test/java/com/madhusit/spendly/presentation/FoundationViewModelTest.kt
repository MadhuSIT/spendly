package com.madhusit.spendly.presentation

import com.madhusit.spendly.MainDispatcherRule
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.FoundationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FoundationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initializePersistsStateAndExposesContent() = runTest {
        val repository = FakeRepository()
        val viewModel = FoundationViewModel(repository)

        viewModel.initialize()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Spendly foundation is ready.", viewModel.uiState.value.message)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun repeatedInitializeDoesNotDuplicatePersistence() = runTest {
        val repository = FakeRepository()
        val viewModel = FoundationViewModel(repository)

        viewModel.initialize()
        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(1, repository.saveCount)
        assertTrue(viewModel.uiState.value.message != null)
    }

    private class FakeRepository : FoundationRepository {
        private val state = MutableStateFlow<FoundationState?>(null)
        var saveCount = 0

        override fun observe(): Flow<FoundationState?> = state

        override suspend fun save(message: String) {
            saveCount++
            state.value = FoundationState(message)
        }
    }
}
