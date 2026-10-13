package com.madhusit.spendly.domain.sms

import java.util.Locale
import kotlin.math.exp

enum class SmsMlPredictionSource { MODEL, SAFETY_RULE }

data class SmsMlPrediction(
    val label: String,
    val confidence: Double,
    val modelVersion: String,
    val source: SmsMlPredictionSource
)

/**
 * Compact, offline statistical classifier. The versioned one-vs-rest log-odds
 * weights are generated from synthetic examples by tools/sms-ml/train_sms_model.py.
 * High-risk, unambiguous negative patterns are safety overrides; low-confidence
 * model output fails closed to AMBIGUOUS rather than creating ledger impact.
 */
class OnDeviceSmsClassifier {
    fun predict(message: SmsMessage): SmsMlPrediction {
        val text = message.body.lowercase(Locale.ROOT)
        safetyOverride(text)?.let { label ->
            return SmsMlPrediction(label, 1.0, SmsMlModelData.VERSION, SmsMlPredictionSource.SAFETY_RULE)
        }

        val tokens = features(text)
        val scores = SmsMlModelData.biases.mapValues { (label, bias) ->
            bias + tokens.sumOf { token -> SmsMlModelData.weights[label]?.get(token) ?: 0.0 }
        }
        val maxScore = scores.values.maxOrNull() ?: 0.0
        val exponentials = scores.mapValues { exp((it.value - maxScore).coerceAtLeast(-60.0)) }
        val denominator = exponentials.values.sum().coerceAtLeast(1e-12)
        val (label, rawConfidence) = exponentials.maxByOrNull { it.value }?.let { it.key to it.value / denominator }
            ?: return ambiguous(0.0)
        if (label == "TRANSACTION" && rawConfidence < MIN_TRANSACTION_CONFIDENCE) return ambiguous(rawConfidence)
        if (label == "AMBIGUOUS" || rawConfidence < MIN_CLASS_CONFIDENCE) return ambiguous(rawConfidence)
        return SmsMlPrediction(label, rawConfidence, SmsMlModelData.VERSION, SmsMlPredictionSource.MODEL)
    }

    private fun features(text: String): Set<String> {
        val tokens = Regex("[a-z0-9]+").findAll(text).map { it.value }.toList()
        return buildSet {
            addAll(tokens)
            tokens.zipWithNext().forEach { (left, right) -> add("${left}_${right}") }
        }
    }

    private fun safetyOverride(text: String): String? {
        val confirmedAction = Regex(
            "\\b(debited|credited|spent|withdrawn|refunded|refund|reversed|charged|paid\\s+to|purchase\\s+of)\\b"
        ).containsMatchIn(text)
        if (listOf("transaction failed", "payment failed", "transaction declined", "payment declined",
                "transaction unsuccessful", "insufficient funds", "insufficient balance",
                "transaction rejected", "not debited", "no money deducted").any { it in text }) return "FAILED"
        if (!confirmedAction && listOf("fraud alert", "suspicious transaction", "suspicious activity",
                "security warning", "security alert", "did you attempt", "if you did not",
                "not initiated by you", "unrecognized transaction").any { it in text }) return "SECURITY_ALERT"
        if (!confirmedAction && listOf("otp", "verification code", "one time password",
                "authentication code").any { it in text }) return "OTP"
        if (!confirmedAction && listOf("due tomorrow", "minimum due", "payment reminder",
                "statement generated", "bill payment is due", "due date", "outstanding amount").any { it in text }) return "REMINDER"
        if (!confirmedAction && listOf("available balance", "account balance", "statement balance",
                "balance enquiry", "credit limit", "balance is").any { it in text }) return "BALANCE"
        if (!confirmedAction && listOf("loan offer", "pre approved", "cashback", "click here",
                "offer expires", "apply now", "subscribe now", "reply stop").any { it in text }) return "MARKETING"
        return null
    }

    private fun ambiguous(confidence: Double) =
        SmsMlPrediction("AMBIGUOUS", confidence, SmsMlModelData.VERSION, SmsMlPredictionSource.MODEL)

    private companion object {
        const val MIN_TRANSACTION_CONFIDENCE = 0.72
        const val MIN_CLASS_CONFIDENCE = 0.52
    }
}
