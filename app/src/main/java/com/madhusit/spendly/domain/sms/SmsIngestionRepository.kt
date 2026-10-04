package com.madhusit.spendly.domain.sms

data class SmsInboxScanSummary(val total: Int, val saved: Int, val review: Int)

interface SmsIngestionRepository {
    suspend fun process(message: SmsMessage): SmsParseResult
    suspend fun scanInbox(
        context: android.content.Context,
        lookbackMs: Long = 7 * 24 * 60 * 60 * 1000L
    ): SmsInboxScanSummary
    suspend fun clearProcessedEvents()
}
