package com.example.calculatorapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.calculatorapp.data.CurrencyRepository

class CurrencySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val repository = CurrencyRepository(applicationContext)
            val base = inputData.getString("base") ?: "USD"
            repository.fetchAndPersist(base)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
