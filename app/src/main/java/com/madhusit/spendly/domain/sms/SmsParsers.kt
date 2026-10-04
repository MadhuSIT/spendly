package com.madhusit.spendly.domain.sms

import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.TransactionCategory
import com.madhusit.spendly.domain.ledger.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

interface SmsParser {
    fun canParse(message: SmsMessage): Boolean
    fun parse(message: SmsMessage): NormalizedSmsTransaction?
}

// ─── Amount extraction ────────────────────────────────────────────────────────
//
// Strategy: try transaction-context patterns in priority order, then reject
// anything that is clearly a balance figure.

private val CURRENCY = """(?:rs\.?\s*|inr\s*|₹\s*)"""
private val NUM     = """([0-9,]+(?:\.\d{1,2})?)"""

// Pass 1 — amount explicitly tied to a debit/spend/paid action
private val AMOUNT_DEBIT_CTX = Regex(
    """(?:debited?|spent|charged?|purchase(?:\s+of)?|payment\s+of)\s+$CURRENCY$NUM""",
    RegexOption.IGNORE_CASE
)
// Pass 2 — "paid Rs X", "for Rs X", "of Rs X"  (money out)
private val AMOUNT_PAID_CTX = Regex(
    """(?:paid|for|of)\s+$CURRENCY$NUM""",
    RegexOption.IGNORE_CASE
)
// Pass 3 — "credited with Rs X", "received Rs X"  (money in)
private val AMOUNT_CREDIT_CTX = Regex(
    """(?:credited(?:\s+with)?|received)\s+$CURRENCY$NUM""",
    RegexOption.IGNORE_CASE
)
// Pass 4 — currency symbol anywhere, but NOT preceded by a balance indicator
private val AMOUNT_ANY = Regex(
    """$CURRENCY$NUM""",
    RegexOption.IGNORE_CASE
)
// Marks a balance figure — amounts matching this should be skipped in pass 4
private val BALANCE_INDICATOR = Regex(
    """(?:avl\.?\s*bal|available\s+bal|a/c\s+bal|closing\s+bal|total\s+due|min(?:imum)?\s+due|balance\s+is|bal\.?:)\s*$CURRENCY$NUM""",
    RegexOption.IGNORE_CASE
)

fun extractAmountMinor(text: String): Long? {
    fun Long.validAmount() = takeIf { it in 1..10_000_000_00L }  // ₹0.01 – ₹1 crore

    // Pass 1-3: context-anchored, inherently trustworthy
    val ctxAmount = (AMOUNT_DEBIT_CTX.find(text)
        ?: AMOUNT_PAID_CTX.find(text)
        ?: AMOUNT_CREDIT_CTX.find(text))
        ?.groupValues?.get(1)
        ?.replace(",", "")
        ?.toBigDecimalOrNull()
        ?.let { (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong() }
        ?.validAmount()
    if (ctxAmount != null) return ctxAmount

    // Pass 4: find all currency amounts, skip balance figures, take the first
    val balanceAmounts = BALANCE_INDICATOR.findAll(text)
        .map { it.groupValues[1].replace(",", "") }
        .toSet()
    return AMOUNT_ANY.findAll(text)
        .map { it.groupValues[1] }
        .firstOrNull { it.replace(",", "") !in balanceAmounts }
        ?.replace(",", "")
        ?.toBigDecimalOrNull()
        ?.let { (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong() }
        ?.validAmount()
}

// ─── Merchant extraction ──────────────────────────────────────────────────────
//
// Priority chain — returns first non-null, non-noise result.

// HDFC/SBI style: "Info: Merchant Name UPI Ref" or "Info: MERCHANT NAME."
private val INFO_FIELD = Regex(
    """(?:\binfo[:\s]+|tran\s+info[:\s]+|ref\s+info[:\s]+)([A-Za-z][A-Za-z0-9 &'./\-]{2,40})""",
    RegexOption.IGNORE_CASE
)
// ICICI/Kotak style: "Rs X debited from acct; MERCHANT NAME. UPI"
private val AFTER_SEMICOLON = Regex(
    """;\s*([A-Za-z][A-Za-z0-9 &'./\-]{2,40?})(?:\.|UPI|\n|$)""",
    RegexOption.IGNORE_CASE
)
// UPI VPA — the local part before "@" is usually the merchant/sender name
private val UPI_VPA = Regex(
    """(?:vpa|merchant\s+vpa|upi\s+vpa)[:\s]+([A-Za-z0-9._\-]{2,30})@[A-Za-z0-9.\-]+""",
    RegexOption.IGNORE_CASE
)
// VPA in free text: word@upihandle — extract the local part if it looks like a merchant
private val VPA_FREETEXT = Regex(
    """(?<![/@])([A-Za-z][A-Za-z0-9.\-]{1,20})@(?:oksbi|okhdfcbank|okicici|okaxis|ybl|upi|paytm|apl|jupiteraxis|freecharge|pockets|idfcbank|sbi|icici|hdfc|axis)\b""",
    RegexOption.IGNORE_CASE
)
// Axis/IDFC style: "to MERCHANT NAME" at end of transaction description
private val TO_MERCHANT = Regex(
    """\bto\s+([A-Z][A-Za-z0-9 &'.]{2,30})(?:\s*(?:via|upi|ref|@|\d)|[.,\n]|$)"""
)
// Generic "at MERCHANT" — stricter than before (requires capital start, stops at digits/time)
private val AT_MERCHANT = Regex(
    """\bat\s+([A-Z][A-Za-z0-9 &'.]{2,30})(?=\s*(?:on|\d\d[-/]|\.|,|$))"""
)

// Words that are never merchant names
private val NOISE_TOKENS = setOf(
    "your", "the", "bank", "account", "acct", "card", "upi", "neft", "rtgs",
    "imps", "ref", "txn", "transaction", "payment", "amount", "inr", "rs",
    "balance", "avail", "available", "net banking", "mobile", "app",
    "hdfc", "icici", "sbi", "axis", "kotak", "paytm", "amazon", "flipkart"
)

fun extractMerchant(text: String): String? {
    fun String.cleaned() = trim().trimEnd('.', ',', ';', '-', '/')
        .replace(Regex("\\s{2,}"), " ")
        .takeIf { it.length >= 2 && it.lowercase() !in NOISE_TOKENS }

    INFO_FIELD.find(text)?.groupValues?.get(1)?.cleaned()?.let { return it }
    AFTER_SEMICOLON.find(text)?.groupValues?.get(1)?.cleaned()?.let { return it }

    // VPA — convert "swiggy@okicici" → "Swiggy", "9876543210@upi" → null (phone number)
    (UPI_VPA.find(text) ?: VPA_FREETEXT.find(text))?.groupValues?.get(1)?.let { local ->
        if (local.all { it.isDigit() || it == '+' || it == '-' }) return@let  // phone VPA
        return local.replace(Regex("[._\\-]"), " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            .cleaned()
    }

    TO_MERCHANT.find(text)?.groupValues?.get(1)?.cleaned()?.let { return it }
    AT_MERCHANT.find(text)?.groupValues?.get(1)?.cleaned()?.let { return it }
    return null
}

// ─── Last-four / masked account ───────────────────────────────────────────────

// X{2,}NNNN — standard masked format used by virtually all Indian banks
private val MASKED_ACCOUNT = Regex("""[Xx]{2,}(\d{4})""")
// "a/c XXXXXX1234" — SBI style with full masking
private val AC_LAST4 = Regex(
    """(?:a/c|acct?|account|card)[^0-9a-z]{0,6}(?:[Xx*]{2,}\s*)?(\d{4})\b""",
    RegexOption.IGNORE_CASE
)

fun extractLastFour(text: String): String? =
    MASKED_ACCOUNT.find(text)?.groupValues?.get(1)
        ?: AC_LAST4.find(text)?.groupValues?.get(1)

// ─── UPI reference ────────────────────────────────────────────────────────────

private val UPI_REF = Regex(
    """(?:upi\s*ref(?:erence)?(?:\s*no\.?)?|txn\s*(?:id|ref)|ref(?:erence)?\s*(?:no\.?|#|id)?)[:\s#\-]*([0-9A-Za-z]{8,22})""",
    RegexOption.IGNORE_CASE
)

fun extractUpiRef(text: String): String? = UPI_REF.find(text)?.groupValues?.get(1)

// ─── Timestamp extraction ─────────────────────────────────────────────────────

private val MONTH_MAP = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

// "03-Oct-25 14:23" / "03-Oct-2025 14:23:45"
internal val DT_NAMED_MONTH = Regex(
    """(\d{1,2})[-/\s]([A-Za-z]{3})[-/\s](\d{2,4})[\s,T]+(\d{1,2}):(\d{2})(?::(\d{2}))?"""
)
// "03/10/2025 14:30:00" (DD/MM/YYYY HH:MM[:SS])
internal val DT_NUMERIC_TIME = Regex(
    """(\d{2})[-/](\d{2})[-/](\d{4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?"""
)
// "03/10/2025" or "03-10-2025" date only
internal val DATE_NUMERIC = Regex("""(\d{2})[-/](\d{2})[-/](\d{4})""")
// "03Oct2025" / "03oct25"
private val DATE_COMPACT = Regex("""(\d{1,2})([A-Za-z]{3})(\d{2,4})""")
// "2025-10-03 14:30" ISO style
private val DT_ISO = Regex("""(\d{4})[-/](\d{2})[-/](\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?""")

fun extractTimestamp(text: String, fallback: Long): Long {
    DT_ISO.find(text)?.groupValues?.let { g ->
        if (g.size >= 6) return epochOf(g[1].toInt(), g[2].toInt(), g[3].toInt(),
            g[4].toInt(), g[5].toInt(), g[6].toIntOrNull() ?: 0)
    }
    DT_NAMED_MONTH.find(text)?.groupValues?.let { g ->
        val month = MONTH_MAP[g[2].lowercase()] ?: return@let
        val year = if (g[3].length == 2) 2000 + g[3].toInt() else g[3].toInt()
        return epochOf(year, month, g[1].toInt(), g[4].toInt(), g[5].toInt(), g[6].toIntOrNull() ?: 0)
    }
    DT_NUMERIC_TIME.find(text)?.groupValues?.let { g ->
        val day = g[1].toInt(); val month = g[2].toInt(); val year = g[3].toInt()
        if (month in 1..12 && day in 1..31)
            return epochOf(year, month, day, g[4].toInt(), g[5].toInt(), g[6].toIntOrNull() ?: 0)
    }
    DATE_NUMERIC.find(text)?.groupValues?.let { g ->
        val day = g[1].toInt(); val month = g[2].toInt(); val year = g[3].toInt()
        if (month in 1..12 && day in 1..31) return epochOf(year, month, day, 0, 0, 0)
    }
    DATE_COMPACT.find(text)?.groupValues?.let { g ->
        val month = MONTH_MAP[g[2].lowercase()] ?: return@let
        val year = if (g[3].length == 2) 2000 + g[3].toInt() else g[3].toInt()
        if (year in 2000..2099) return epochOf(year, month, g[1].toInt(), 0, 0, 0)
    }
    return fallback
}

private fun epochOf(year: Int, month: Int, day: Int, h: Int, min: Int, sec: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    cal.set(year, month - 1, day, h, min, sec)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ─── Transaction type detection ───────────────────────────────────────────────

fun detectType(text: String): TransactionType {
    val lower = text.lowercase(Locale.ROOT)

    // Explicitly non-actionable states (parse as non-financial at caller level)
    // These should be blocked in SmsDetector, but guard here too
    if ("failed" in lower || "declined" in lower || "insufficient" in lower || "rejected" in lower)
        return TransactionType.EXPENSE  // caller will drop these

    return when {
        "refund" in lower || "reversal" in lower || "reversed" in lower -> TransactionType.REFUND
        "withdraw" in lower || ("atm" in lower && ("debit" in lower || "cash" in lower)) -> TransactionType.CASH_WITHDRAWAL
        // Fee/charge — but "service charge" is different from "charged Rs X"
        ("service charge" in lower || "annual fee" in lower || "late payment fee" in lower
            || "finance charge" in lower || "processing fee" in lower) -> TransactionType.FEE
        // Credit card bill payment (not a purchase)
        ("credit card" in lower || " cc " in lower) &&
            ("bill" in lower || "due" in lower || "minimum due" in lower || "payment" in lower) -> TransactionType.CARD_PAYMENT
        // Fund transfer keywords
        ("neft" in lower || "rtgs" in lower || "imps" in lower || "transfer" in lower
            || "transferred" in lower || "to a/c" in lower || "to acct" in lower) &&
            ("debit" in lower || "credited" !in lower) -> TransactionType.TRANSFER
        // Debit = money out
        "debited" in lower || "debit" in lower || "spent" in lower || "paid" in lower
            || "purchase" in lower || "pos " in lower || "payment" in lower -> TransactionType.EXPENSE
        // Credit = money in (but guard against credit card usage alerts)
        ("credited" in lower || "credit" in lower || "received" in lower)
            && "credit card" !in lower && "cc " !in lower -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }
}

// ─── Entity type hint ─────────────────────────────────────────────────────────

fun detectEntityType(body: String): FinancialEntityType {
    val lower = body.lowercase(Locale.ROOT)
    return when {
        "credit card" in lower || " cc " in lower || "creditcard" in lower
            || "cc no" in lower || "cc a/c" in lower -> FinancialEntityType.CREDIT_CARD
        "debit card" in lower || " dc " in lower -> FinancialEntityType.DEBIT_CARD
        else -> FinancialEntityType.BANK_ACCOUNT
    }
}

// ─── Category detection ───────────────────────────────────────────────────────

private fun inText(text: String, vararg kw: String) = kw.any { it in text }

fun detectCategory(merchantName: String?, body: String, type: TransactionType): TransactionCategory {
    val t = ((merchantName ?: "") + " " + body).lowercase(Locale.ROOT)
    return when {
        type == TransactionType.REFUND || type == TransactionType.REVERSAL -> TransactionCategory.REFUND
        type == TransactionType.CASH_WITHDRAWAL -> TransactionCategory.ATM_CASH
        type == TransactionType.FEE -> TransactionCategory.FEE_CHARGE
        type == TransactionType.TRANSFER -> TransactionCategory.TRANSFER
        type == TransactionType.CARD_PAYMENT -> TransactionCategory.FEE_CHARGE
        inText(t, "salary", "payroll", "payslip", "ctc", "stipend", "wages") -> TransactionCategory.SALARY
        inText(t, "swiggy", "zomato", "uber eat", "ubereat", "dominos", "domino's", "kfc",
            "mcdonald", "pizza hut", "restaurant", "café", "cafe", "biryani", "dhaba",
            "food", "bakery", "bake", "eat", "meals") -> TransactionCategory.FOOD_DINING
        inText(t, "bigbasket", "big basket", "zepto", "blinkit", "grofers", "dmart", "d-mart",
            "fresh to home", "milk basket", "grocery", "supermart", "vegetables",
            "supermarket", "bazaar", "kirana") -> TransactionCategory.GROCERIES
        inText(t, "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal",
            "tata cliq", "reliance digital", "croma", "vijay sales", "jiomart",
            "shopping", "retail") -> TransactionCategory.SHOPPING
        inText(t, "uber", "ola", "rapido", "irctc", "metro", "indigo", "air india",
            "spicejet", "vistara", "akasa", "goair", "bus ticket", "redbus",
            "train", "railway", "cab", "auto", "rickshaw") -> TransactionCategory.TRANSPORT
        inText(t, "iocl", "hpcl", "bpcl", "indian oil", "bharat petroleum",
            "petrol", "diesel", "fuel", "hp petrol") -> TransactionCategory.FUEL
        inText(t, "netflix", "spotify", "hotstar", "prime video", "youtube premium",
            "jiocinema", "zee5", "sony liv", "apple music", "gaana", "wynk",
            "bookmyshow", "paytm insider", "district") -> TransactionCategory.ENTERTAINMENT
        inText(t, "apollo", "fortis", "practo", "medplus", "pharmeasy", "1mg", "netmeds",
            "hospital", "clinic", "pharmacy", "chemist", "medical", "health",
            "doctor", "diagnostic", "lab test") -> TransactionCategory.HEALTHCARE
        inText(t, "jio recharge", "airtel recharge", "bsnl", "vodafone", "vi recharge",
            "electricity", "bescom", "tpddl", "msedcl", "water bill", "gas bill",
            "piped gas", "broadband", "wifi", "internet", "recharge") -> TransactionCategory.UTILITIES
        inText(t, "college", "university", "school", "tuition", "coaching",
            "byju", "unacademy", "coursera", "udemy", "exam fee", "admission") -> TransactionCategory.EDUCATION
        inText(t, "hotel", "oyo", "makemytrip", "goibibo", "airbnb", "booking.com",
            "cleartrip", "yatra", "holiday", "resort", "hostel") -> TransactionCategory.TRAVEL
        type == TransactionType.INCOME -> TransactionCategory.INCOME_OTHER
        else -> TransactionCategory.OTHER
    }
}

// ─── Confidence scoring ───────────────────────────────────────────────────────

fun scoreConfidence(
    isKnownProvider: Boolean,
    hasLastFour: Boolean,
    hasMerchant: Boolean,
    hasUpiRef: Boolean,
    hasTimestampInBody: Boolean
): Double {
    var score = if (isKnownProvider) 0.70 else 0.50
    if (hasLastFour) score += 0.12
    if (hasMerchant) score += 0.08
    if (hasUpiRef) score += 0.05
    if (hasTimestampInBody) score += 0.05
    return score.coerceIn(0.0, 1.0)
}

// ─── Provider parser ──────────────────────────────────────────────────────────

class ProviderSmsParser(
    private val provider: String,
    private val sourcePattern: Regex,
    private val parserVersion: String = "3"
) : SmsParser {

    override fun canParse(message: SmsMessage) =
        sourcePattern.containsMatchIn(message.sender) ||
            message.body.contains(provider, ignoreCase = true)

    override fun parse(message: SmsMessage): NormalizedSmsTransaction? {
        val text = message.body
        val lower = text.lowercase(Locale.ROOT)

        // Drop failed/declined/OTP-only messages
        if (isNonTransaction(lower)) return null

        val amountMinor = extractAmountMinor(text) ?: return null
        val type = detectType(text)
        val lastFour = extractLastFour(text)
        val upiRef = extractUpiRef(text)
        val merchant = extractMerchant(text)
        val bodyHasTimestamp = hasTimestampInBody(text)
        val timestamp = extractTimestamp(text, message.receivedAtEpochMillis)
        val entityType = detectEntityType(text)
        val category = detectCategory(merchant, text, type)

        val confidence = scoreConfidence(
            isKnownProvider = provider != "GENERIC",
            hasLastFour = lastFour != null,
            hasMerchant = merchant != null,
            hasUpiRef = upiRef != null,
            hasTimestampInBody = bodyHasTimestamp
        )

        return NormalizedSmsTransaction(
            type = type,
            amountMinor = amountMinor,
            currency = "INR",
            merchantName = merchant,
            provider = provider,
            maskedIdentifier = lastFour?.let { "XX$it" },
            lastFour = lastFour,
            transactionTimestamp = timestamp,
            referenceNumber = upiRef,
            upiReference = if ("upi" in lower) upiRef else null,
            parserSource = provider.lowercase(Locale.ROOT),
            parserVersion = parserVersion,
            confidence = confidence,
            category = category,
            entityType = entityType,
            reviewRequired = confidence < 0.80
        )
    }

    private fun isNonTransaction(lower: String): Boolean =
        ("failed" in lower || "declined" in lower || "unsuccessful" in lower)
            && "refund" !in lower

    private fun hasTimestampInBody(text: String): Boolean =
        DT_NAMED_MONTH.containsMatchIn(text) ||
            DT_NUMERIC_TIME.containsMatchIn(text) ||
            DATE_NUMERIC.containsMatchIn(text)
}

// ─── Generic fallback ─────────────────────────────────────────────────────────

class GenericSmsParser : SmsParser {
    override fun canParse(message: SmsMessage) = true
    override fun parse(message: SmsMessage): NormalizedSmsTransaction? =
        ProviderSmsParser("GENERIC", Regex(".*"), "3").parse(message)?.copy(
            parserSource = "generic",
            confidence = 0.60,
            reviewRequired = true
        )
}
