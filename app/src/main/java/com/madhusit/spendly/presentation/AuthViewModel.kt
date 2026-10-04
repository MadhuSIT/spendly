package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.auth.AuthRepository
import com.madhusit.spendly.domain.auth.AuthUser
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.sms.SmsIngestionRepository
import com.madhusit.spendly.domain.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val GUEST_USER = AuthUser(uid = "guest", email = null, displayName = "Guest", photoUrl = null)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val ledgerRepository: LedgerRepository,
    private val smsIngestionRepository: SmsIngestionRepository
) : ViewModel() {

    private val _guestMode = MutableStateFlow(false)

    val user: StateFlow<AuthUser?> = combine(authRepository.currentUser, _guestMode) { fbUser, isGuest ->
        fbUser ?: if (isGuest) GUEST_USER else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.getCurrentUser())

    var syncError: String? = null
        private set

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                ledgerRepository.clearAllData()
                smsIngestionRepository.clearProcessedEvents()
                pullFromCloud(user.uid)
            }.onFailure {
                syncError = it.message
            }
        }
    }

    fun signInAsGuest() {
        _guestMode.value = true
    }

    fun signOut() {
        _guestMode.value = false
        viewModelScope.launch { authRepository.signOut() }
    }

    fun pushTransaction(txn: com.madhusit.spendly.domain.ledger.LedgerTransaction) {
        val uid = user.value?.uid?.takeIf { it != "guest" } ?: return
        viewModelScope.launch {
            runCatching { syncRepository.pushTransaction(uid, txn) }
        }
    }

    fun pushEntity(entity: com.madhusit.spendly.domain.ledger.FinancialEntity) {
        val uid = user.value?.uid?.takeIf { it != "guest" } ?: return
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
            ledgerRepository: LedgerRepository,
            smsIngestionRepository: SmsIngestionRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(authRepository, syncRepository, ledgerRepository, smsIngestionRepository) as T
        }
    }
}
