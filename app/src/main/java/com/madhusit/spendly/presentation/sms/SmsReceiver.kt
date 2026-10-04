package com.madhusit.spendly.presentation.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.madhusit.spendly.SpendlyApplication
import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.presentation.notification.SpendlyNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "Spendly.SmsReceiver"

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val app = context.applicationContext as SpendlyApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                Log.d(TAG, "SMS count=${messages.size}")
                messages.forEach { sms ->
                    val body = sms.messageBody.orEmpty()
                    Log.d(TAG, "Processing from=${sms.originatingAddress} body=${body.take(60)}")
                    val result = app.smsIngestionRepository.process(
                        SmsMessage(
                            sender = sms.originatingAddress.orEmpty(),
                            body = body,
                            receivedAtEpochMillis = sms.timestampMillis
                        )
                    )
                    Log.d(TAG, "Result cls=${result.classification} fail=${result.failureReason}")
                    // Notify for every successfully parsed real-time transaction
                    val norm = result.normalized ?: return@forEach
                    if (result.failureReason == null) {
                        SpendlyNotificationHelper.notifyTransaction(
                            context = context,
                            amountMinor = norm.amountMinor,
                            currency = norm.currency,
                            merchantName = norm.merchantName,
                            type = norm.type,
                            category = norm.category,
                            transactionId = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
