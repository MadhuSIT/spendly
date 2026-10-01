package com.madhusit.spendly.domain.ledger

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    CARD_PAYMENT,
    REFUND,
    FEE,
    CASH_WITHDRAWAL,
    REVERSAL
}