package com.madhusit.spendly.domain.sms

interface SmsIngestionRepository {
    suspend fun process(message: SmsMessage): SmsParseResult
    suspend fun scanInbox(context: android.content.Context, lookbackMs: Long = 7 * 24 * 60 * 60 * 1000L)
    suspend fun clearProcessedEvents()
}
