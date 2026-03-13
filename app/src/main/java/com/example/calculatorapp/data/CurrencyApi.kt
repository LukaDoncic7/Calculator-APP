package com.example.calculatorapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class CurrencyResponse(
    @SerialName("base_code") val baseCode: String,
    @SerialName("time_last_update_unix") val updatedAtUnix: Long,
    val rates: Map<String, Double>
)

class CurrencyApi {
    private val json = Json { ignoreUnknownKeys = true }

    fun getLatestRates(base: String): CurrencyResponse {
        val endpoint = "https://open.er-api.com/v6/latest/$base"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.connect()

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("No se pudieron obtener tasas: ${connection.responseCode}")
        }

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString(body)
    }
}
