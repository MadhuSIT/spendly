package com.madhusit.spendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.madhusit.spendly.presentation.SpendlyApp
import com.madhusit.spendly.presentation.theme.SpendlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpendlyTheme {
                val app = application as SpendlyApplication
                SpendlyApp(
                    repository = app.repository,
                    ledgerRepository = app.ledgerRepository,
                    authRepository = app.authRepository,
                    syncRepository = app.syncRepository,
                    smsIngestionRepository = app.smsIngestionRepository
                )
            }
        }
    }
}
