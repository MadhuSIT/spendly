package com.madhusit.spendly.presentation.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.madhusit.spendly.SpendlyApplication
import com.madhusit.spendly.domain.sms.SmsMessage
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
                    Log.d(TAG, "Processing SMS from=${sms.originatingAddress} body=${sms.messageBody.take(60)}")
                    val result = app.smsIngestionRepository.process(
                        SmsMessage(
                            sender = sms.originatingAddress.orEmpty(),
                            body = sms.messageBody.orEmpty(),
                            receivedAtEpochMillis = sms.timestampMillis
                        )
                    )
                    Log.d(TAG, "Result classification=${result.classification} failure=${result.failureReason} normalized=${result.normalized != null}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

}
