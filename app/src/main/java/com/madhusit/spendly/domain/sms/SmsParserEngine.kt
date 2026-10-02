package com.madhusit.spendly.domain.sms

class SmsParserEngine(
    private val detector: SmsDetector = SmsDetector(),
    private val parsers: List<SmsParser> = listOf(
        ProviderSmsParser("HDFC", Regex("HDFCBK|HDFC", RegexOption.IGNORE_CASE)),
        ProviderSmsParser("SBI", Regex("SBI|SBIPSG", RegexOption.IGNORE_CASE)),
        ProviderSmsParser("AXIS", Regex("AXIS", RegexOption.IGNORE_CASE)),
        ProviderSmsParser("ICICI", Regex("ICICI", RegexOption.IGNORE_CASE)),
        ProviderSmsParser("AMEX", Regex("AMEX", RegexOption.IGNORE_CASE)),
        GenericSmsParser()
    )
) {
    fun parse(message: SmsMessage): SmsParseResult {
        if (detector.classify(message) != SmsClassification.FINANCIAL) {
            return SmsParseResult(SmsClassification.NON_FINANCIAL)
        }
        val parser = parsers.firstOrNull { it.canParse(message) } ?: return SmsParseResult(
            SmsClassification.FINANCIAL, failureReason = SmsFailureReason.UNSUPPORTED_PROVIDER
        )
        val normalized = parser.parse(message)
            ?: return SmsParseResult(SmsClassification.FINANCIAL, failureReason = SmsFailureReason.UNPARSEABLE)
        if (normalized.amountMinor <= 0 || normalized.currency.length != 3 || normalized.confidence !in 0.0..1.0) {
            return SmsParseResult(SmsClassification.FINANCIAL, failureReason = SmsFailureReason.INVALID_NORMALIZED_RESULT)
        }
        return SmsParseResult(SmsClassification.FINANCIAL, normalized = normalized)
    }
}
