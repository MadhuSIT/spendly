package com.madhusit.spendly.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.madhusit.spendly.MainActivity
import com.madhusit.spendly.domain.ledger.TransactionCategory
import com.madhusit.spendly.domain.ledger.TransactionType

object SpendlyNotificationHelper {

    private const val CHANNEL_TXN   = "spendly_transactions"
    private const val CHANNEL_BATCH = "spendly_batch_import"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TXN, "Transactions", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Real-time transaction alerts from SMS"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BATCH, "Import summary", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Summary after SMS inbox scan"
            }
        )
    }

    fun notifyTransaction(
        context: Context,
        amountMinor: Long,
        currency: String,
        merchantName: String?,
        type: TransactionType,
        category: TransactionCategory?,
        transactionId: String?
    ) {
        val title = when (type) {
            TransactionType.INCOME    -> "Money received"
            TransactionType.REFUND    -> "Refund credited"
            TransactionType.REVERSAL  -> "Transaction reversed"
            TransactionType.TRANSFER  -> "Transfer recorded"
            TransactionType.CASH_WITHDRAWAL -> "ATM withdrawal"
            else -> "New expense"
        }
        val amountStr = formatAmount(amountMinor, currency)
        val body = if (merchantName != null) "$amountStr at $merchantName" else amountStr
        val icon = categoryIcon(category)

        notify(
            context = context,
            notifId = System.currentTimeMillis().toInt(),
            channelId = CHANNEL_TXN,
            title = "$icon $title",
            body = body,
            intent = mainIntent(context, transactionId?.let { "transaction/$it" })
        )
    }

    fun notifyBatchImport(context: Context, total: Int, saved: Int, review: Int) {
        if (total == 0) return
        val body = when {
            saved > 0 && review > 0 -> "$saved saved, $review need your review"
            saved > 0               -> "$saved transactions imported"
            review > 0              -> "$review transactions need your review"
            else                    -> "$total messages scanned, nothing new"
        }
        val intent = mainIntent(context, if (review > 0) "review_queue" else null)
        notify(context, 99_001, CHANNEL_BATCH, "SMS Import", body, intent)
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun notify(
        context: Context,
        notifId: Int,
        channelId: String,
        title: String,
        body: String,
        intent: PendingIntent
    ) {
        val nm = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            nm.areNotificationsEnabled().not()
        ) return

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try { nm.notify(notifId, notif) } catch (_: SecurityException) { /* permission not yet granted */ }
    }

    private fun mainIntent(context: Context, deepLinkPath: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLinkPath?.let { putExtra("deep_link", it) }
        }
        return PendingIntent.getActivity(
            context,
            deepLinkPath.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatAmount(minor: Long, currency: String): String {
        val major = minor / 100.0
        return if (currency == "INR") "₹%.2f".format(major) else "%.2f $currency".format(major)
    }

    private fun categoryIcon(category: TransactionCategory?) = when (category) {
        TransactionCategory.FOOD_DINING  -> "🍽"
        TransactionCategory.GROCERIES    -> "🛒"
        TransactionCategory.SHOPPING     -> "🛍"
        TransactionCategory.TRANSPORT    -> "🚗"
        TransactionCategory.FUEL         -> "⛽"
        TransactionCategory.ENTERTAINMENT -> "🎬"
        TransactionCategory.HEALTHCARE   -> "💊"
        TransactionCategory.UTILITIES    -> "💡"
        TransactionCategory.EDUCATION    -> "📚"
        TransactionCategory.TRAVEL       -> "✈"
        TransactionCategory.SALARY       -> "💼"
        TransactionCategory.ATM_CASH     -> "🏧"
        TransactionCategory.REFUND       -> "↩"
        TransactionCategory.TRANSFER     -> "↔"
        else -> "💳"
    }
}
