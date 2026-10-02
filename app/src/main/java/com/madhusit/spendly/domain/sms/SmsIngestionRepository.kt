package com.madhusit.spendly.domain.sms

interface SmsIngestionRepository {
    suspend fun process(message: SmsMessage): SmsParseResult
}
