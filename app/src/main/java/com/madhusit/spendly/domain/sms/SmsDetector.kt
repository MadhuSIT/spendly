package com.madhusit.spendly.domain.sms

/**
 * Uses the compact on-device statistical classifier before any amount parser
 * can run. Non-transaction alerts are blocked; uncertain messages remain
 * ambiguous and therefore cannot create an automatic ledger transaction.
 */
class SmsDetector(
    private val classifier: OnDeviceSmsClassifier = OnDeviceSmsClassifier()
) {
    fun predict(message: SmsMessage): SmsMlPrediction = classifier.predict(message)

    fun classify(message: SmsMessage): SmsClassification = classify(predict(message))

    fun classify(prediction: SmsMlPrediction): SmsClassification = when (prediction.label) {
        "TRANSACTION" -> SmsClassification.FINANCIAL
        "AMBIGUOUS" -> SmsClassification.AMBIGUOUS
        else -> SmsClassification.NON_FINANCIAL
    }
}
