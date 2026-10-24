package com.madhusit.spendly.domain.sms

import com.madhusit.spendly.domain.ledger.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * [SmsParser] backed by the on-device ONNX MobileBERT extractor. It is tried
 * FIRST by [SmsParserEngine], but is strictly advisory: it only returns a
 * normalized transaction for messages the model confidently reads as a real
 * TRANSACTION, and reuses the deterministic field extractors for anything the
 * model does not provide. For every other case it returns null so the engine
 * falls through to the regex provider parsers. It never resolves accounts,
 * deduplicates, or writes the ledger.
 */
class OnnxSmsParser(
    private val extractor: OnnxSmsExtractor,
) : SmsParser {

    override fun canParse(message: SmsMessage): Boolean = true

    override fun parse(message: SmsMessage): NormalizedSmsTransaction? {
        val text = message.body
        val lower = text.lowercase(Locale.ROOT)

        // Safety: never turn a failed/declined message into a ledger entry.
        if (("failed" in lower || "declined" in lower || "unsuccessful" in lower) && "refund" !in lower) {
            return null
        }

        val result = extractor.extract(text) ?: return null
        // Only act on confident transactions; let regex handle the rest.
        if (result.intent != "TRANSACTION") return null

        val slots = result.slots
        val amountMinor = slots["AMOUNT"]?.let { amountToMinor(it) }
            ?: extractAmountMinor(text)
            ?: return null

        val type = detectType(text)
        val merchant = slots["MERCHANT"]?.cleanMerchant() ?: extractMerchant(text)
        val lastFour = slots["ACCOUNT_LAST4"]?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            ?: extractLastFour(text)
        val reference = slots["REFERENCE"] ?: extractUpiRef(text)
        val timestamp = extractTimestamp(text, message.receivedAtEpochMillis)
        val entityType = detectEntityType(text)
        val category = detectCategory(merchant, text, type)

        val confidence = run {
            var score = 0.74
            if (lastFour != null) score += 0.10
            if (merchant != null) score += 0.06
            if (reference != null) score += 0.05
            score.coerceIn(0.0, 1.0)
        }

        return NormalizedSmsTransaction(
            type = type,
            amountMinor = amountMinor,
            currency = "INR",
            merchantName = merchant,
            provider = detectProvider(message.sender, text),
            maskedIdentifier = lastFour?.let { "XX$it" },
            lastFour = lastFour,
            transactionTimestamp = timestamp,
            referenceNumber = reference,
            upiReference = if ("upi" in lower) reference else null,
            parserSource = "onnx-mobilebert",
            parserVersion = extractor.modelVersion,
            confidence = confidence,
            category = category,
            entityType = entityType,
            reviewRequired = confidence < 0.80,
        )
    }

    private fun amountToMinor(raw: String): Long? =
        raw.replace(",", "").toBigDecimalOrNull()
            ?.let { (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong() }
            ?.takeIf { it in 1..10_000_000_00L }

    private fun String.cleanMerchant(): String? =
        trim().trimEnd('.', ',', ';', '-', '/').replace(Regex("\\s{2,}"), " ")
            .takeIf { it.length >= 2 }
}
