package com.madhusit.spendly

import android.app.Application
import androidx.room.Room
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.repository.FoundationRepositoryImpl
import com.madhusit.spendly.domain.FoundationRepository

class SpendlyApplication : Application() {
    val database: SpendlyDatabase by lazy {
        Room.databaseBuilder(this, SpendlyDatabase::class.java, "spendly.db").build()
    }
    val repository: FoundationRepository by lazy {
        FoundationRepositoryImpl(database.foundationDao())
    }
}
