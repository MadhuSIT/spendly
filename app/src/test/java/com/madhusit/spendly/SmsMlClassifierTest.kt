package com.madhusit.spendly

import com.madhusit.spendly.domain.sms.OnDeviceSmsClassifier
import com.madhusit.spendly.domain.sms.SmsClassification
import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.domain.sms.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMlClassifierTest {
    private val classifier = OnDeviceSmsClassifier()
    private val engine = SmsParserEngine()

    private fun message(body: String) = SmsMessage("UNKNOWN", body, 1_760_000_000_000L)

    @Test
    fun confirmedDebitIsClassifiedAsFinancialAndParsed() {
        val sms = message("Rs 500 debited from your account via UPI to ABC Store ref 123456789")
        val prediction = classifier.predict(sms)
        assertEquals("TRANSACTION", prediction.label)
        assertTrue(prediction.confidence >= 0.72)
        assertNotNull(prediction.modelVersion)
        assertEquals(SmsClassification.FINANCIAL, engine.parse(sms).classification)
        assertEquals(50000L, engine.parse(sms).normalized?.amountMinor)
    }

    @Test
    fun fraudWarningMentioningAnAmountNeverBecomesExpense() {
        val sms = message("Fraud alert: suspicious transaction detected for Rs 10000. If not you call bank immediately")
        assertEquals("SECURITY_ALERT", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
        assertEquals(null, engine.parse(sms).normalized)
    }

    @Test
    fun otpContainingTransactionAmountNeverBecomesExpense() {
        val sms = message("Your OTP for Rs 10000 transaction is 123456. Do not share it")
        assertEquals("OTP", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
    }

    @Test
    fun creditCardDueReminderIsNotAnExpense() {
        val sms = message("Your credit card payment of Rs 5000 is due tomorrow")
        assertEquals("REMINDER", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
    }

    @Test
    fun balanceOnlyMessageIsNotAnExpense() {
        val sms = message("Available balance in your account is INR 42300")
        assertEquals("BALANCE", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
    }

    @Test
    fun failedTransactionIsNotAnExpense() {
        val sms = message("Your UPI transaction failed for Rs 500. Amount not debited")
        assertEquals("FAILED", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
    }

    @Test
    fun vagueSuccessfulPaymentRequiresReviewInsteadOfFinancialImport() {
        val sms = message("Transaction of Rs 500 successful")
        assertEquals("AMBIGUOUS", classifier.predict(sms).label)
        assertEquals(SmsClassification.AMBIGUOUS, engine.parse(sms).classification)
    }

    @Test
    fun classifierIsOfflineVersionedAndDeterministic() {
        val sms = message("Rs 250 debited from account at local store")
        val first = classifier.predict(sms)
        val second = classifier.predict(sms)
        assertEquals(first, second)
        assertTrue(first.modelVersion.startsWith("sms-nb-"))
    }

    @Test
    fun marketingOfferContainingPurchaseLanguageIsNotAnExpense() {
        val sms = message("Get cashback on your next purchase click here to claim offer")
        assertEquals("MARKETING", classifier.predict(sms).label)
        assertEquals(SmsClassification.NON_FINANCIAL, engine.parse(sms).classification)
    }

    @Test
    fun confirmedDebitWithStandardFraudBoilerplateRemainsARealTransaction() {
        val sms = message("Rs 5000 debited from your account. If you did not make this transaction, call bank fraud team")
        assertEquals(SmsClassification.FINANCIAL, engine.parse(sms).classification)
        assertEquals(500000L, engine.parse(sms).normalized?.amountMinor)
    }
}
