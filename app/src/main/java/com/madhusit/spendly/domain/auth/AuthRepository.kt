package com.madhusit.spendly.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    fun getCurrentUser(): AuthUser?
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>
    suspend fun signOut()
}
