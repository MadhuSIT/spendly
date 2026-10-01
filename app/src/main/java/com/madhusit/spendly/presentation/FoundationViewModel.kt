package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.SaveFoundationState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoundationUiState(
    val loading: Boolean = true,
    val message: String? = null
)

class FoundationViewModel(private val repository: FoundationRepository) : ViewModel() {
    private val saveState = SaveFoundationState(repository)
    val uiState: StateFlow<FoundationUiState> = repository.observe()
        .map { FoundationUiState(loading = false, message = it?.message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoundationUiState())

    fun initialize() {
        if (uiState.value.message == null) {
            viewModelScope.launch { saveState("Spendly foundation is ready.") }
        }
    }

    companion object {
        fun factory(repository: FoundationRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FoundationViewModel(repository) as T
        }
    }
}
