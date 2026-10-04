package com.madhusit.spendly.domain.sms

data class SmsInboxScanSummary(val total: Int, val saved: Int, val review: Int)

interface SmsIngestionRepository {
    suspend fun process(message: SmsMessage): SmsParseResult
    suspend fun scanInbox(
        context: android.content.Context,
        fromMs: Long = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000,
        toMs: Long = System.currentTimeMillis()
    ): SmsInboxScanSummary
    suspend fun clearProcessedEvents()
}
