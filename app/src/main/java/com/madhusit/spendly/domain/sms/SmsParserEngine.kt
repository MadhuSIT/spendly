package com.madhusit.spendly.domain.sms

class SmsParserEngine(
    private val detector: SmsDetector = SmsDetector(),
    private val parsers: List<SmsParser> = listOf(
        // ── Major private banks ──────────────────────────────────────────────
        ProviderSmsParser("HDFC",       Regex("HDFCBK|HDFCB|HDFC",      RegexOption.IGNORE_CASE)),
        ProviderSmsParser("ICICI",      Regex("ICICIB?|ICICI",           RegexOption.IGNORE_CASE)),
        ProviderSmsParser("AXIS",       Regex("AXISBK|AXIS",             RegexOption.IGNORE_CASE)),
        ProviderSmsParser("KOTAK",      Regex("KOTAK|KOTAKB",            RegexOption.IGNORE_CASE)),
        ProviderSmsParser("IDFCFIRST",  Regex("IDFCFIRST|IDFCBK|IDFC",  RegexOption.IGNORE_CASE)),
        ProviderSmsParser("YESBANK",    Regex("YESBNK|YESBANK",         RegexOption.IGNORE_CASE)),
        ProviderSmsParser("INDUSIND",   Regex("INDUSIN|INDUSIND",        RegexOption.IGNORE_CASE)),
        ProviderSmsParser("RBL",        Regex("RBLBANK|RBLBNK",          RegexOption.IGNORE_CASE)),
        // ── Public sector banks ──────────────────────────────────────────────
        ProviderSmsParser("SBI",        Regex("SBIPSG|SBI",              RegexOption.IGNORE_CASE)),
        ProviderSmsParser("CANARA",     Regex("CNRB|CANARABNK|CANARA",  RegexOption.IGNORE_CASE)),
        ProviderSmsParser("BOB",        Regex("BARODA|BOBINDIA|BOB",     RegexOption.IGNORE_CASE)),
        ProviderSmsParser("PNB",        Regex("PNBSMS|PNBMOB|PNB",      RegexOption.IGNORE_CASE)),
        ProviderSmsParser("FEDERAL",    Regex("FEDBNK|FEDERAL",          RegexOption.IGNORE_CASE)),
        ProviderSmsParser("UNIONBANK",  Regex("UBNKIN|UNIONBK|UNIONBANK",RegexOption.IGNORE_CASE)),
        ProviderSmsParser("IOBK",       Regex("IOBSMS|IOBANKL|IOB",     RegexOption.IGNORE_CASE)),
        // ── Payment banks / wallets ──────────────────────────────────────────
        ProviderSmsParser("PAYTM",      Regex("PYTMBNK|PAYTM",           RegexOption.IGNORE_CASE)),
        ProviderSmsParser("AMEX",       Regex("AMEX|AMERICANEXPRESS",    RegexOption.IGNORE_CASE)),
        // ── Generic fallback (always last) ───────────────────────────────────
        GenericSmsParser()
    )
) {
    fun parse(message: SmsMessage): SmsParseResult {
        val prediction = detector.predict(message)
        val classification = detector.classify(prediction)
        when (classification) {
            SmsClassification.NON_FINANCIAL ->
                return SmsParseResult(
                    SmsClassification.NON_FINANCIAL,
                    classifierLabel = prediction.label,
                    classifierConfidence = prediction.confidence,
                    classifierVersion = prediction.modelVersion
                )
            SmsClassification.AMBIGUOUS ->
                return SmsParseResult(
                    SmsClassification.AMBIGUOUS,
                    failureReason = SmsFailureReason.AMBIGUOUS_TRANSACTION,
                    classifierLabel = prediction.label,
                    classifierConfidence = prediction.confidence,
                    classifierVersion = prediction.modelVersion
                )
            SmsClassification.FINANCIAL -> Unit
        }

        val parser = parsers.firstOrNull { it.canParse(message) }
            ?: return SmsParseResult(SmsClassification.FINANCIAL, failureReason = SmsFailureReason.UNSUPPORTED_PROVIDER, classifierLabel = prediction.label, classifierConfidence = prediction.confidence, classifierVersion = prediction.modelVersion)
        val normalized = parser.parse(message)
            ?: return SmsParseResult(SmsClassification.FINANCIAL, failureReason = SmsFailureReason.UNPARSEABLE, classifierLabel = prediction.label, classifierConfidence = prediction.confidence, classifierVersion = prediction.modelVersion)
        if (normalized.amountMinor <= 0 || normalized.currency.length != 3 || normalized.confidence !in 0.0..1.0) {
            return SmsParseResult(SmsClassification.FINANCIAL, failureReason = SmsFailureReason.INVALID_NORMALIZED_RESULT, classifierLabel = prediction.label, classifierConfidence = prediction.confidence, classifierVersion = prediction.modelVersion)
        }
        return SmsParseResult(SmsClassification.FINANCIAL, normalized = normalized, classifierLabel = prediction.label, classifierConfidence = prediction.confidence, classifierVersion = prediction.modelVersion)
    }
}
