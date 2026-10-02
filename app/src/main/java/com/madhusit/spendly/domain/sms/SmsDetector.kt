package com.madhusit.spendly.domain.sms

class SmsDetector {
    private val positive = listOf(
        "debited", "credited", "spent", "paid", "purchase", "transaction",
        "payment", "withdrawn", "received", "refund", "reversed", "upi", "card", "pos", "atm"
    )
    private val negative = listOf(
        "otp", "verification code", "promotional", "offer", "loan offer",
        "login alert", "available balance", "current balance"
    )
    private val strongFinancial = listOf(
        "debited", "credited", "spent", "purchase", "transaction",
        "withdrawn", "refund", "reversed", "received", "upi"
    )

    fun classify(message: SmsMessage): SmsClassification {
        val text = message.body.lowercase()
        if (("otp" in text || "verification code" in text) &&
            strongFinancial.none(text::contains)
        ) {
            return SmsClassification.NON_FINANCIAL
        }
        if (negative.any(text::contains) &&
            !strongFinancial.any(text::contains)
        ) {
            return SmsClassification.NON_FINANCIAL
        }
        if (positive.any(text::contains)) return SmsClassification.FINANCIAL
        return SmsClassification.NON_FINANCIAL
    }
}
