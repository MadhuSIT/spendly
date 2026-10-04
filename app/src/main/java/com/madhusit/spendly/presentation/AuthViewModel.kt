package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.auth.AuthRepository
import com.madhusit.spendly.domain.auth.AuthUser
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.sync.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    val user: StateFlow<AuthUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.getCurrentUser())

    var syncError: String? = null
        private set

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                pullFromCloud(user.uid)
            }.onFailure {
                syncError = it.message
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun pushTransaction(txn: com.madhusit.spendly.domain.ledger.LedgerTransaction) {
        val uid = user.value?.uid ?: return
        viewModelScope.launch {
            runCatching { syncRepository.pushTransaction(uid, txn) }
        }
    }

    fun pushEntity(entity: com.madhusit.spendly.domain.ledger.FinancialEntity) {
        val uid = user.value?.uid ?: return
        viewModelScope.launch {
            runCatching { syncRepository.pushEntity(uid, entity) }
        }
    }

    private suspend fun pullFromCloud(uid: String) {
        runCatching {
            val result = syncRepository.pullAll(uid)
            result.entities.forEach { ledgerRepository.createEntity(it) }
            result.transactions.forEach { ledgerRepository.createTransaction(it) }
        }.onFailure {
            syncError = it.message
        }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            syncRepository: SyncRepository,
            ledgerRepository: LedgerRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(authRepository, syncRepository, ledgerRepository) as T
        }
    }
}
