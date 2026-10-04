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

// ─── Amount extraction ───────────────────────────────────────────────────────

private val AMOUNT_IN_CONTEXT = Regex(
    """(?:debited?|credited?|paid?|spent|purchase(?:\s+of)?|amount(?:\s+of)?|charged?|for|of)\s+(?:rs\.?\s*|inr\s*|₹\s*)([0-9,]+(?:\.\d{1,2})?)""",
    RegexOption.IGNORE_CASE
)
private val AMOUNT_CURRENCY_PREFIX = Regex(
    """(?:rs\.?\s*|inr\s*|₹\s*)([0-9,]+(?:\.\d{1,2})?)""",
    RegexOption.IGNORE_CASE
)

fun extractAmountMinor(text: String): Long? {
    val raw = AMOUNT_IN_CONTEXT.find(text)?.groupValues?.get(1)
        ?: AMOUNT_CURRENCY_PREFIX.find(text)?.groupValues?.get(1)
        ?: return null
    val decimal = raw.replace(",", "").toBigDecimalOrNull() ?: return null
    return (decimal * BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()
        .takeIf { it > 0 }
}

// ─── Timestamp extraction ────────────────────────────────────────────────────

private val MONTH_MAP = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

// "03-Oct-25 14:23" or "03-Oct-2025 14:23:45"
private val DT_NAMED_MONTH = Regex(
    """(\d{1,2})[-/\s]([A-Za-z]{3})[-/\s](\d{2,4})[\s,T]+(\d{1,2}):(\d{2})(?::(\d{2}))?"""
)
// "03/10/2025" or "03-10-2025" (DD/MM/YYYY)
private val DT_NUMERIC_WITH_TIME = Regex(
    """(\d{2})[-/](\d{2})[-/](\d{4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?"""
)
// "03/10/2025" or "03-10-2025" date only
private val DATE_NUMERIC = Regex("""(\d{2})[-/](\d{2})[-/](\d{4})""")
// "03Oct2025" or "03oct25"
private val DATE_COMPACT = Regex("""(\d{2})([A-Za-z]{3})(\d{2,4})""")

fun extractTimestamp(text: String, fallback: Long): Long {
    DT_NAMED_MONTH.find(text)?.let { m ->
        val g = m.groupValues
        val month = MONTH_MAP[g[2].lowercase()] ?: return@let
        val year = if (g[3].length == 2) 2000 + g[3].toInt() else g[3].toInt()
        return epochOf(year, month, g[1].toInt(), g[4].toInt(), g[5].toInt(), g[6].toIntOrNull() ?: 0)
    }
    DT_NUMERIC_WITH_TIME.find(text)?.let { m ->
        val g = m.groupValues
        return epochOf(g[3].toInt(), g[2].toInt(), g[1].toInt(), g[4].toInt(), g[5].toInt(), g[6].toIntOrNull() ?: 0)
    }
    DATE_NUMERIC.find(text)?.let { m ->
        val g = m.groupValues
        val day = g[1].toInt(); val month = g[2].toInt(); val year = g[3].toInt()
        if (month in 1..12 && day in 1..31) return epochOf(year, month, day, 0, 0, 0)
    }
    DATE_COMPACT.find(text)?.let { m ->
        val g = m.groupValues
        val month = MONTH_MAP[g[2].lowercase()] ?: return@let
        val year = if (g[3].length == 2) 2000 + g[3].toInt() else g[3].toInt()
        return epochOf(year, month, g[1].toInt(), 0, 0, 0)
    }
    return fallback
}

private fun epochOf(year: Int, month: Int, day: Int, h: Int, min: Int, sec: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    cal.set(year, month - 1, day, h, min, sec)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ─── Category detection ──────────────────────────────────────────────────────

private fun inText(text: String, vararg kw: String) = kw.any { it in text }

fun detectCategory(merchantName: String?, body: String, type: TransactionType): TransactionCategory {
    val t = ((merchantName ?: "") + " " + body).lowercase(Locale.ROOT)
    return when {
        type == TransactionType.REFUND || type == TransactionType.REVERSAL -> TransactionCategory.REFUND
        type == TransactionType.CASH_WITHDRAWAL -> TransactionCategory.ATM_CASH
        type == TransactionType.FEE -> TransactionCategory.FEE_CHARGE
        type == TransactionType.TRANSFER -> TransactionCategory.TRANSFER
        type == TransactionType.CARD_PAYMENT -> TransactionCategory.FEE_CHARGE
        inText(t, "salary", "payroll", "ctc", "stipend") -> TransactionCategory.SALARY
        inText(t, "swiggy", "zomato", "uber eat", "domino", "kfc", "mcdonald", "pizza", "restaurant", "café", "cafe", "biryani", "dhaba", "food", "bake") -> TransactionCategory.FOOD_DINING
        inText(t, "bigbasket", "zepto", "blinkit", "grofer", "dmart", "fresh to home", "milk", "grocery", "vegetables", "supermarket", "bazaar") -> TransactionCategory.GROCERIES
        inText(t, "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal", "tata cliq", "reliance", "croma", "vijay sales") -> TransactionCategory.SHOPPING
        inText(t, "uber", "ola", "rapido", "irctc", "metro", "indigo", "air india", "spicejet", "vistara", "goair", "akasa", "bus ticket", "redbus") -> TransactionCategory.TRANSPORT
        inText(t, "iocl", "hpcl", "bpcl", "hp petrol", "indian oil", "bharat petrol", "petrol", "diesel", "fuel") -> TransactionCategory.FUEL
        inText(t, "netflix", "spotify", "hotstar", "prime video", "youtube", "jiocinema", "zee5", "sun nxt", "sony liv", "apple music", "gaana") -> TransactionCategory.ENTERTAINMENT
        inText(t, "apollo", "fortis", "practo", "medplus", "pharmeasy", "1mg", "netmeds", "hospital", "clinic", "pharmacy", "medical", "health", "doctor") -> TransactionCategory.HEALTHCARE
        inText(t, "jio", "airtel recharge", "bsnl", "vodafone", "vi recharge", "electricity", "bescom", "water bill", "gas bill", "broadband", "wifi", "internet", "recharge") -> TransactionCategory.UTILITIES
        inText(t, "college", "university", "school", "tuition", "coaching", "byju", "unacademy", "coursera", "udemy", "fee") -> TransactionCategory.EDUCATION
        inText(t, "hotel", "oyo", "makemytrip", "goibibo", "airbnb", "booking.com", "cleartrip", "yatra") -> TransactionCategory.TRAVEL
        type == TransactionType.INCOME -> TransactionCategory.INCOME_OTHER
        else -> TransactionCategory.OTHER
    }
}

// ─── Entity type hint ────────────────────────────────────────────────────────

fun detectEntityType(body: String): FinancialEntityType {
    val lower = body.lowercase(Locale.ROOT)
    return when {
        "credit card" in lower || " cc " in lower || "creditcard" in lower || "cc no" in lower -> FinancialEntityType.CREDIT_CARD
        "debit card" in lower || " dc " in lower -> FinancialEntityType.DEBIT_CARD
        else -> FinancialEntityType.BANK_ACCOUNT
    }
}

// ─── Shared parse helpers ────────────────────────────────────────────────────

private val LAST_FOUR = Regex("""(?:a/c|card|acct|acc|xx+|x{2,})[^0-9]{0,8}(\d{4})""", RegexOption.IGNORE_CASE)
private val UPI_REF = Regex("""(?:upi\s*ref(?:erence)?|txn\s*id|ref\s*no?|ref#?)[\s:#\-]*([A-Za-z0-9]{6,20})""", RegexOption.IGNORE_CASE)
private val MERCHANT_AT = Regex("""(?:\bat\b|\bto\b|\bfrom\b)\s+([A-Za-z][A-Za-z0-9 &'./@-]{2,35})""", RegexOption.IGNORE_CASE)

// ─── Provider parser ─────────────────────────────────────────────────────────

class ProviderSmsParser(
    private val provider: String,
    private val sourcePattern: Regex,
    private val parserVersion: String = "2"
) : SmsParser {

    override fun canParse(message: SmsMessage) =
        sourcePattern.containsMatchIn(message.sender) ||
            message.body.lowercase(Locale.ROOT).contains(provider.lowercase(Locale.ROOT))

    override fun parse(message: SmsMessage): NormalizedSmsTransaction? {
        val text = message.body
        val lower = text.lowercase(Locale.ROOT)

        val amountMinor = extractAmountMinor(text) ?: return null

        val type = detectType(lower)
        val lastFour = LAST_FOUR.find(text)?.groupValues?.get(1)
        val upiRef = UPI_REF.find(text)?.groupValues?.get(1)
        val merchant = MERCHANT_AT.find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.', ',', ';')
        val timestamp = extractTimestamp(text, message.receivedAtEpochMillis)
        val entityType = detectEntityType(text)
        val category = detectCategory(merchant, text, type)

        return NormalizedSmsTransaction(
            type = type,
            amountMinor = amountMinor,
            currency = "INR",
            merchantName = merchant,
            provider = provider,
            maskedIdentifier = lastFour?.let { "XXXX$it" },
            lastFour = lastFour,
            transactionTimestamp = timestamp,
            referenceNumber = upiRef,
            upiReference = if ("upi" in lower) upiRef else null,
            parserSource = provider.lowercase(Locale.ROOT),
            parserVersion = parserVersion,
            confidence = if (lastFour != null) 0.93 else 0.83,
            category = category,
            entityType = entityType,
            reviewRequired = lastFour == null
        )
    }

    private fun detectType(lower: String): TransactionType = when {
        "refund" in lower -> TransactionType.REFUND
        "revers" in lower -> TransactionType.REVERSAL
        "withdraw" in lower || ("atm" in lower && "debit" in lower) -> TransactionType.CASH_WITHDRAWAL
        "fee" in lower || ("charge" in lower && "credit" !in lower) -> TransactionType.FEE
        // Credit card payment must be checked BEFORE generic "credit" check
        ("credit card" in lower || "cc " in lower) && ("bill" in lower || "due" in lower || "payment" in lower) -> TransactionType.CARD_PAYMENT
        "transfer" in lower || "neft" in lower || "rtgs" in lower || "imps" in lower || "to a/c" in lower -> TransactionType.TRANSFER
        // Debit = money out
        "debit" in lower || "debited" in lower || "spent" in lower || "paid" in lower || "purchase" in lower -> TransactionType.EXPENSE
        // Credit = money in — but only if not a credit card usage alert
        ("credit" in lower || "credited" in lower || "received" in lower) && "credit card" !in lower -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }
}

// ─── Generic fallback parser ─────────────────────────────────────────────────

class GenericSmsParser : SmsParser {
    override fun canParse(message: SmsMessage) = true
    override fun parse(message: SmsMessage): NormalizedSmsTransaction? =
        ProviderSmsParser("GENERIC", Regex(".*"), "2").parse(message)?.copy(
            parserSource = "generic",
            confidence = 0.65,
            reviewRequired = true
        )
}
