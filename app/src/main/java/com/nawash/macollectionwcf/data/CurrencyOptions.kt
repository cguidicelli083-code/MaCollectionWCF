package com.nawash.macollectionwcf.data

/** Devise affichée/éditable (la cote reste stockée en euros ; conversion à l'affichage uniquement). */
data class CurrencyOption(val code: String, val symbol: String, val label: String)

object CurrencyOptions {
    val list: List<CurrencyOption> = listOf(
        CurrencyOption("EUR", "€", "Euro"),
        CurrencyOption("USD", "$", "Dollar US"),
        CurrencyOption("GBP", "£", "Livre sterling"),
        CurrencyOption("JPY", "¥", "Yen japonais"),
        CurrencyOption("CHF", "CHF", "Franc suisse"),
        CurrencyOption("CAD", "$", "Dollar canadien"),
        CurrencyOption("AUD", "$", "Dollar australien"),
        CurrencyOption("BRL", "R$", "Real brésilien"),
        CurrencyOption("MXN", "$", "Peso mexicain"),
        CurrencyOption("TRY", "₺", "Livre turque"),
        CurrencyOption("RUB", "₽", "Rouble russe"),
        CurrencyOption("CNY", "¥", "Yuan chinois")
    )

    fun symbolFor(code: String): String = list.firstOrNull { it.code == code }?.symbol ?: code

    /** Devise par défaut associée à la langue de l'appli (modifiable ensuite à tout moment). */
    fun defaultForLanguage(tag: String): String = when (tag) {
        "en" -> "GBP"
        "ru" -> "RUB"
        "tr" -> "TRY"
        "ja" -> "JPY"
        "zh" -> "CNY"
        else -> "EUR"
    }
}
