package com.madhusit.spendly.domain.sms

/**
 * High-recall financial SMS gate. Priority: NEVER drop a real transaction.
 *
 * We only hard-block messages that are clearly non-financial (OTP without any
 * money movement, failed/declined, pure promotional noise). Everything that
 * carries a currency amount plus any account/card/UPI/transaction structure is
 * treated as FINANCIAL and allowed into parsing (which, with the deterministic
 * pipeline + review routing, is safe to over-include — false positives can be
 * reviewed/deleted, a missed debit cannot).
 */
class SmsDetector {

    // Clear money-movement verbs → definitely financial.
    private val strongFinancial = listOf(
        "debited", "credited", "debit", "credit", "spent", "purchase", "purchased",
        "withdrawn", "withdrawal", "refund", "refunded", "reversed", "received",
        "sent", "paid", "deducted", "transferred", "charged", "upi ref", "txn id",
    )
    // Softer money words; financial when paired with a currency amount or structure.
    private val softFinancial = listOf(
        "payment", "transaction", "txn", "transfer", "pos ", "atm", "neft",
        "rtgs", "imps", "upi", "emi", "autopay", "mandate", "bill",
    )
    // Any structural indicator of a real account/card/UPI/transaction.
    private val structure = Regex(
        """[xX*]{2,}\d{2,4}|\ba/c\b|\bacct\b|\baccount\b|\bcard\b|\bvpa\b|@[a-z]|\bupi\b|""" +
            """\bref(?:erence)?\b|\btxn\b|\butr\b|\brrn\b|\bimps\b|\bneft\b|\brtgs\b""",
        RegexOption.IGNORE_CASE,
    )
    private val currencyInBody = Regex("""(?:rs\.?\s*|inr\s*|₹\s*)\d""", RegexOption.IGNORE_CASE)

    private val hardNegative = listOf("otp", "verification code", "one time password", "one-time password")
    private val softNegative = listOf(
        "loan offer", "pre-approved", "pre approved", "congratulations", "offer expires",
        "click here", "download", "subscribe", "register now", "visit us",
        "lowest interest", "apply now", "reply stop",
    )
    private val failedTransaction = listOf(
        "transaction failed", "payment failed", "transaction declined",
        "payment declined", "transaction unsuccessful", "could not process",
        "insufficient funds", "insufficient balance", "transaction rejected",
        "was not processed", "was declined", "has failed",
    )

    fun classify(message: SmsMessage): SmsClassification {
        val text = message.body.lowercase()
        val hasStrong = strongFinancial.any { it in text }
        val hasCurrency = currencyInBody.containsMatchIn(text)
        val hasStructure = structure.containsMatchIn(message.body)
        // Only a clear money-movement verb overrides an OTP block (an OTP SMS can
        // itself say "received"/"sent", so those must NOT override it).
        val clearMovement = listOf(
            "debited", "credited", "spent", "withdrawn", "charged", "deducted", "purchase",
        ).any { it in text }

        // OTP: block unless there is clear debit/credit movement.
        if (hardNegative.any { it in text } && !clearMovement) return SmsClassification.NON_FINANCIAL
        // Failed/declined: financial-shaped but nothing to record.
        if (failedTransaction.any { it in text }) return SmsClassification.NON_FINANCIAL
        // Promotional noise: block only when no strong money-movement verb.
        if (softNegative.any { it in text } && !hasStrong) return SmsClassification.NON_FINANCIAL

        // Strong verb → financial.
        if (hasStrong) return SmsClassification.FINANCIAL
        // High recall: a currency amount plus any transaction structure or soft
        // money word is enough. Covers "...using Card ending 1234 for Rs 500 at
        // AMAZON", "Rs 500 to abc@upi", etc. that lack an explicit verb.
        if (hasCurrency && (hasStructure || softFinancial.any { it in text })) {
            return SmsClassification.FINANCIAL
        }
        return SmsClassification.NON_FINANCIAL
    }
}
