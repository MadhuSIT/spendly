package com.madhusit.spendly.data.local.ledger

import androidx.room.TypeConverter
import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.TransactionStatus
import com.madhusit.spendly.domain.ledger.TransactionType

class LedgerConverters {
    @TypeConverter fun entityTypeToString(value: FinancialEntityType): String = value.name
    @TypeConverter fun stringToEntityType(value: String): FinancialEntityType = FinancialEntityType.valueOf(value)
    @TypeConverter fun transactionTypeToString(value: TransactionType): String = value.name
    @TypeConverter fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun transactionStatusToString(value: TransactionStatus): String = value.name
    @TypeConverter fun stringToTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)
}