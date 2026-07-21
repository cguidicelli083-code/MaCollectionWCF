package com.nawash.macollectionwcf.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

/**
 * Prix de référence (côte indicative) vérifiés sur ShandoraShop pour des figurines WCF précises,
 * fournis par l'utilisateur le 2026-07-19 (mêmes pages produit que [CatalogReferencePhotos]).
 * Contrairement aux photos, ces prix sont ceux d'un revendeur d'import (neuf, prix de vente),
 * donc distincts d'une cote d'occasion eBay — appliqués UNIQUEMENT si la fiche n'a encore aucun
 * prix connu ([FigureCatalogDao.setReferencePriceIfMissing]), jamais pour écraser une cote eBay
 * déjà résolue. Rapprochement par (série, personnage) normalisés, même convention que
 * [CatalogReferencePhotos] (minuscule + suppression des caractères non alphanumériques).
 */
object CatalogReferencePrices {
    private const val ASSET_NAME = "catalog_prices.json"

    private data class PriceRef(val series: String, val character: String, val priceCents: Int)

    private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "")

    suspend fun applyAll(context: Context, dao: FigureCatalogDao): Int {
        val refs: List<PriceRef> = try {
            context.assets.open(ASSET_NAME).use { input ->
                val json = input.bufferedReader(Charsets.UTF_8).readText()
                Gson().fromJson(json, object : TypeToken<List<PriceRef>>() {}.type)
            }
        } catch (e: Exception) {
            return 0
        }
        if (refs.isEmpty()) return 0

        val allEntries = dao.observeAll().first()
        val bySlug = allEntries.groupBy { slug(it.series) to slug(it.character) }

        var applied = 0
        for (ref in refs) {
            val entry = bySlug[ref.series to ref.character]?.firstOrNull() ?: continue
            if (entry.priceCents == null) {
                dao.setReferencePriceIfMissing(entry.id, ref.priceCents)
                applied++
            }
        }
        return applied
    }
}
