package com.example.calculatorapp.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CurrencyState(
    val base: String = "USD",
    val rates: Map<String, Double> = emptyMap(),
    val updatedAtMillis: Long = 0L,
    val error: String? = null
)

class CurrencyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("currency_cache", Context.MODE_PRIVATE)
    private val api = CurrencyApi()
    private val json = Json

    fun loadCachedRates(): CurrencyState {
        val base = prefs.getString("base", "USD") ?: "USD"
        val ratesJson = prefs.getString("rates", null)
        val updated = prefs.getLong("updated", 0L)
        if (ratesJson.isNullOrBlank()) return CurrencyState(base = base)

        return runCatching {
            val rates = json.decodeFromString<Map<String, Double>>(ratesJson)
            CurrencyState(base = base, rates = rates, updatedAtMillis = updated)
        }.getOrElse {
            CurrencyState(base = base, error = "Cache inválido")
        }
    }

    fun fetchAndPersist(base: String): CurrencyState {
        val response = api.getLatestRates(base)
        prefs.edit()
            .putString("base", response.baseCode)
            .putString("rates", json.encodeToString(response.rates))
            .putLong("updated", response.updatedAtUnix * 1000)
            .apply()

        return CurrencyState(
            base = response.baseCode,
            rates = response.rates,
            updatedAtMillis = response.updatedAtUnix * 1000
        )
    }
}
