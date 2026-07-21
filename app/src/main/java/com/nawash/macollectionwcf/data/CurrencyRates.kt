package com.nawash.macollectionwcf.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private data class RatesResponse(val result: String?, val rates: Map<String, Double>?)

private interface RatesApi {
    @GET("v6/latest/EUR")
    suspend fun latest(): RatesResponse
}

/**
 * Taux de change en direct (base EUR, devise de stockage des cotes), via une API gratuite sans
 * clé. Best-effort : en cas d'échec (pas de réseau...), on garde les derniers taux connus
 * (mis en cache par [AppPrefs]) plutôt que de planter ou d'afficher des cotes fausses.
 */
object CurrencyRates {
    private val api: RatesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RatesApi::class.java)
    }

    suspend fun fetchLatest(): Map<String, Double>? = try {
        api.latest().rates
    } catch (e: Exception) {
        null
    }
}
