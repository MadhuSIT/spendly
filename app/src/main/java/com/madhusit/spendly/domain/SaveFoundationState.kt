package com.madhusit.spendly.domain

class SaveFoundationState(private val repository: FoundationRepository) {
    suspend operator fun invoke(message: String) = repository.save(message)
}
