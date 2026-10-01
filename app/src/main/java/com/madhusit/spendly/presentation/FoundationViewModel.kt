package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.SaveFoundationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoundationUiState(
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

class FoundationViewModel(private val repository: FoundationRepository) : ViewModel() {
    private val saveState = SaveFoundationState(repository)
    private val _uiState = MutableStateFlow(FoundationUiState())
    val uiState: StateFlow<FoundationUiState> = _uiState.asStateFlow()
    private var initializationRequested = false

    init {
        viewModelScope.launch {
            repository.observe().collect { state ->
                if (state != null) {
                    _uiState.value = FoundationUiState(
                        loading = false,
                        message = state.message
                    )
                    initializationRequested = true
                }
            }
        }
    }

    fun initialize() {
        if (initializationRequested) return
        initializationRequested = true
        _uiState.value = FoundationUiState(loading = true)

        viewModelScope.launch {
            runCatching {
                saveState("Spendly foundation is ready.")
            }.onFailure { error ->
                initializationRequested = false
                _uiState.value = FoundationUiState(
                    loading = false,
                    error = error.message ?: "Unable to save Spendly state."
                )
            }
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
