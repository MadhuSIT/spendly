package com.madhusit.spendly.presentation.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.madhusit.spendly.SpendlyApplication
import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.domain.sms.SmsParseResult
import com.madhusit.spendly.presentation.notification.SpendlyNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "Spendly.SmsReceiver"

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val app = context.applicationContext as SpendlyApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                Log.d(TAG, "Received SMS event with count=${messages.size}")
                messages.forEach { sms ->
                    val result = processMessage(
                        app,
                        SmsMessage(
                            sender = sms.originatingAddress.orEmpty(),
                            body = sms.messageBody.orEmpty(),
                            receivedAtEpochMillis = sms.timestampMillis
                        )
                    )
                    Log.d(TAG, "Processed SMS event: class=${result.classification}, failure=${result.failureReason}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Shared processing path for the Android receiver and integration tests.
     * Raw message contents and sender identifiers must never be written to logs.
     */
    internal suspend fun processMessage(
        app: SpendlyApplication,
        message: SmsMessage
    ): SmsParseResult {
        val result = app.smsIngestionRepository.process(message)
        val normalized = result.normalized ?: return result
        if (result.failureReason == null) {
            SpendlyNotificationHelper.notifyTransaction(
                context = app,
                amountMinor = normalized.amountMinor,
                currency = normalized.currency,
                merchantName = normalized.merchantName,
                type = normalized.type,
                category = normalized.category,
                transactionId = null
            )
        }
        return result
    }
}
