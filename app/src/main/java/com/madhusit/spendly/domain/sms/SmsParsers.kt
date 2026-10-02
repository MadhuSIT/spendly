package com.madhusit.spendly.domain.sms

import com.madhusit.spendly.domain.ledger.TransactionType
import java.math.BigDecimal
import java.util.Locale

interface SmsParser {
    fun canParse(message: SmsMessage): Boolean
    fun parse(message: SmsMessage): NormalizedSmsTransaction?
}

class ProviderSmsParser(
    private val provider: String,
    private val sourcePattern: Regex,
    private val parserVersion: String = "1"
) : SmsParser {
    override fun canParse(message: SmsMessage) = sourcePattern.containsMatchIn(message.sender) || message.body.lowercase().contains(provider.lowercase())

    override fun parse(message: SmsMessage): NormalizedSmsTransaction? {
        val text = message.body
        val lower = text.lowercase()
        val amount = Regex("""(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val amountMinor = amount.movePointRight(2).longValueExact()
        val type = when {
            "refund" in lower -> TransactionType.REFUND
            "revers" in lower -> TransactionType.REVERSAL
            "withdraw" in lower || "atm" in lower -> TransactionType.CASH_WITHDRAWAL
            "fee" in lower || "charge" in lower -> TransactionType.FEE
            "credit" in lower || "received" in lower -> TransactionType.INCOME
            "payment" in lower && "card" in lower && ("bill" in lower || "due" in lower) -> TransactionType.CARD_PAYMENT
            "transfer" in lower || "to a/c" in lower -> TransactionType.TRANSFER
            "failed" in lower || "declined" in lower || "rejected" in lower -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }
        val lastFour = Regex("""(?:a/c|card|xx|x{2,})[^0-9]{0,8}(\d{4})""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)
        val upi = Regex("""(?:upi|ref(?:erence)?|txn(?: id)?)[\s:#-]*([A-Za-z0-9]{6,})""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)
        val merchant = Regex("""(?:at|to|from)\s+([A-Za-z][A-Za-z0-9 .&'-]{2,40})""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.', ',')
        return NormalizedSmsTransaction(
            type = type,
            amountMinor = amountMinor,
            currency = "INR",
            merchantName = merchant,
            provider = provider,
            maskedIdentifier = lastFour?.let { "XXXX$it" },
            lastFour = lastFour,
            transactionTimestamp = message.receivedAtEpochMillis,
            referenceNumber = upi,
            upiReference = if ("upi" in lower) upi else null,
            parserSource = provider.lowercase(Locale.ROOT),
            parserVersion = parserVersion,
            confidence = if (lastFour != null) 0.92 else 0.82,
            reviewRequired = lastFour == null
        )
    }
}

class GenericSmsParser : SmsParser {
    override fun canParse(message: SmsMessage) = true
    override fun parse(message: SmsMessage): NormalizedSmsTransaction? =
        ProviderSmsParser("GENERIC", Regex(".*"), "1").parse(message)?.copy(
            parserSource = "generic",
            confidence = 0.65,
            reviewRequired = true
        )
}
