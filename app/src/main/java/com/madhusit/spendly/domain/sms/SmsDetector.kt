package com.madhusit.spendly.domain.sms

class SmsDetector {

    // Any of these → definitely financial
    private val strongFinancial = listOf(
        "debited", "credited", "spent", "purchase", "withdrawn",
        "refund", "reversed", "received", "upi ref", "txn id"
    )
    // Soft financial signals (need at least one strong signal too, or be standalone with currency)
    private val softFinancial = listOf(
        "payment", "transaction", "transfer", "pos ", "atm", "neft", "rtgs", "imps"
    )
    // Currency pattern in body
    private val currencyInBody = Regex("""(?:rs\.?\s*|inr\s*|₹\s*)\d""", RegexOption.IGNORE_CASE)

    // Explicit non-financial — these kill the message unless a strong signal overrides
    private val hardNegative = listOf("otp", "verification code", "one time password")
    // Soft negative — noise unless financial signals are strong
    private val softNegative = listOf(
        "loan offer", "pre-approved", "congratulations", "offer expires",
        "click here", "download", "subscribe", "register now",
        "visit us", "call us", "reply stop"
    )
    // Failed transactions — financial in structure but nothing to record
    private val failedTransaction = listOf(
        "transaction failed", "payment failed", "transaction declined",
        "payment declined", "transaction unsuccessful", "could not process",
        "insufficient funds", "insufficient balance", "transaction rejected"
    )

    fun classify(message: SmsMessage): SmsClassification {
        val text = message.body.lowercase()

        // Hard block: OTP messages (unless they also contain a real transaction)
        if (hardNegative.any { it in text } && strongFinancial.none { it in text })
            return SmsClassification.NON_FINANCIAL

        // Block: failed/declined transactions — they're financial-shaped but have nothing to import
        if (failedTransaction.any { it in text })
            return SmsClassification.NON_FINANCIAL

        // Block: promotional noise
        if (softNegative.any { it in text } && strongFinancial.none { it in text })
            return SmsClassification.NON_FINANCIAL

        // Pass: strong financial keyword
        if (strongFinancial.any { it in text }) return SmsClassification.FINANCIAL

        // Pass: soft financial + currency amount in body
        if (softFinancial.any { it in text } && currencyInBody.containsMatchIn(text))
            return SmsClassification.FINANCIAL

        return SmsClassification.NON_FINANCIAL
    }
}
