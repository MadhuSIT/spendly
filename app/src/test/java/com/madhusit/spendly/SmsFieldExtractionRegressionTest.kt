package com.madhusit.spendly

import com.madhusit.spendly.domain.ledger.TransactionType
import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.domain.sms.SmsParserEngine
import com.madhusit.spendly.domain.sms.extractAmountMinor
import com.madhusit.spendly.domain.sms.extractLastFour
import com.madhusit.spendly.domain.sms.extractMerchant
import com.madhusit.spendly.domain.sms.extractUpiRef
import com.madhusit.spendly.domain.sms.detectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Field-level regression coverage for SMS extraction.
 *
 * These tests deliberately assert fields separately from the classifier label:
 * a correctly classified transaction can still be unusable if a field is wrong.
 * Keep examples synthetic and never put real inbox messages or identifiers here.
 */
class SmsFieldExtractionRegressionTest {
    @Test
    fun extractsAmountAsMinorUnitsAcrossCurrencyFormats() {
        assertEquals(125050L, extractAmountMinor("INR 1,250.50 debited from your account"))
        assertEquals(50000L, extractAmountMinor("Rs. 500.00 spent at a store"))
        assertEquals(9900L, extractAmountMinor("₹99 credited to your account"))
    }

    @Test
    fun doesNotTreatAvailableBalanceAsTheTransactionAmount() {
        assertEquals(
            125000L,
            extractAmountMinor("INR 1,250 debited from account. Available balance INR 9,000")
        )
    }

    @Test
    fun keepsTransactionAmountWhenItEqualsTheReportedBalance() {
        // Equal numeric values must not cause the transaction amount to be discarded.
        assertEquals(
            125000L,
            extractAmountMinor("INR 1,250 debited from account. Available balance INR 1,250")
        )
    }

    @Test
    fun extractsMaskedAccountLastFourAndReference() {
        assertEquals("4321", extractLastFour("A/c XX4321 debited for UPI"))
        assertEquals("ABCD12345678", extractUpiRef("UPI Ref: ABCD12345678"))
    }

    @Test
    fun extractsMerchantFromUpiVpaWithoutTreatingPhoneVpaAsMerchant() {
        assertEquals("Swiggy", extractMerchant("Payment to swiggy@okicici completed"))
        assertEquals(null, extractMerchant("Payment to 9876543210@upi completed"))
    }

    @Test
    fun distinguishesRefundTransferAndCardBillPaymentFromExpense() {
        assertEquals(TransactionType.REFUND, detectType("INR 500 refunded to your account"))
        assertEquals(TransactionType.TRANSFER, detectType("INR 500 debited for NEFT transfer"))
        assertEquals(TransactionType.CARD_PAYMENT, detectType("Credit card bill payment of INR 500 received"))
    }

    @Test
    fun parserReturnsAmountAndIdentityFieldsTogetherForKnownProvider() {
        val result = SmsParserEngine().parse(
            SmsMessage(
                sender = "HDFCBK",
                body = "Rs. 321.45 debited from A/c XX4321 to swiggy@okicici. UPI Ref 987654321",
                receivedAtEpochMillis = 1_760_000_000_000L
            )
        )

        assertNotNull(result.normalized)
        assertEquals(32145L, result.normalized?.amountMinor)
        assertEquals("4321", result.normalized?.lastFour)
        assertEquals("987654321", result.normalized?.referenceNumber)
        assertEquals("Swiggy", result.normalized?.merchantName)
    }
}
