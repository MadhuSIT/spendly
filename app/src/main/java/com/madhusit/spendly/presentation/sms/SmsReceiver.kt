package com.madhusit.spendly.presentation.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.madhusit.spendly.SpendlyApplication
import com.madhusit.spendly.domain.sms.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val app = context.applicationContext as SpendlyApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages.forEach { sms ->
                    app.smsIngestionRepository.process(
                        SmsMessage(
                            sender = sms.originatingAddress.orEmpty(),
                            body = sms.messageBody.orEmpty(),
                            receivedAtEpochMillis = sms.timestampMillis
                        )
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
