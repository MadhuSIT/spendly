package com.madhusit.spendly

import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.domain.sms.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsParserMaskedAccountTest {
    @Test
    fun parsesLastFourFromMaskedAccountAfterAccountMarker() {
        val result = SmsParserEngine().parse(
            SmsMessage(
                sender = "HDFCBK",
                body = "Rs. 321.45 debited from A/c XX4321 at SMS E2E Merchant. UPI Ref 987654321",
                receivedAtEpochMillis = 1L
            )
        )

        assertEquals("4321", result.normalized?.lastFour)
        assertEquals(false, result.normalized?.reviewRequired)
    }
}
