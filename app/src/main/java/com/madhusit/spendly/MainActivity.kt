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
            SpendlyTheme { SpendlyApp((application as SpendlyApplication).repository) }
        }
    }
}
