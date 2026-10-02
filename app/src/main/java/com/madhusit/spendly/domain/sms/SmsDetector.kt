package com.madhusit.spendly.domain.sms

class SmsDetector {
    private val positive = listOf("debited", "credited", "spent", "paid", "purchase", "transaction", "payment", "withdrawn", "received", "refund", "reversed", "upi", "card", "pos", "atm")
    private val negative = listOf("otp", "verification code", "promotional", "offer", "loan offer", "login alert", "available balance", "current balance")

    fun classify(message: SmsMessage): SmsClassification {
        val text = message.body.lowercase()
        if (negative.any(text::contains) && !listOf("debited", "credited", "purchase", "payment", "transaction").any(text::contains)) {
            return SmsClassification.NON_FINANCIAL
        }
        if (positive.any(text::contains)) return SmsClassification.FINANCIAL
        return SmsClassification.NON_FINANCIAL
    }
}
