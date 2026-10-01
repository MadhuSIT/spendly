package com.madhusit.spendly.background
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
class FoundationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
