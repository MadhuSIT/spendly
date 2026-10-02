package com.madhusit.spendly

import com.madhusit.spendly.domain.sms.SmsClassification
import com.madhusit.spendly.domain.sms.SmsDetector
import com.madhusit.spendly.domain.sms.SmsMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsDetectorTest {
    private val detector = SmsDetector()

    @Test
    fun financialDebitIsDetected() {
        assertEquals(
            SmsClassification.FINANCIAL,
            detector.classify(SmsMessage("HDFCBK", "Rs. 500 debited from A/c XX1234", 1L))
        )
    }

    @Test
    fun otpAndPromotionalMessagesAreRejected() {
        assertEquals(
            SmsClassification.NON_FINANCIAL,
            detector.classify(SmsMessage("HDFCBK", "OTP 123456 for card payment is valid for 10 minutes", 1L))
        )
        assertEquals(
            SmsClassification.NON_FINANCIAL,
            detector.classify(SmsMessage("HDFCBK", "Get 20% promotional offer on your card", 2L))
        )
    }

    @Test
    fun balanceOnlyMessageIsRejected() {
        assertEquals(
            SmsClassification.NON_FINANCIAL,
            detector.classify(SmsMessage("HDFCBK", "Your available balance is Rs. 10,000", 1L))
        )
    }
}
