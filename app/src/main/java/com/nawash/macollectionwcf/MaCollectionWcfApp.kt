package com.nawash.macollectionwcf

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

/**
 * Wikimedia (upload.wikimedia.org) exige un en-tête User-Agent descriptif et bloque (403)
 * les requêtes avec un agent générique/vide. Coil utilise sinon l'agent par défaut d'OkHttp ;
 * on en force un explicite pour toutes les images de l'app.
 */
class MaCollectionWcfApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MaCollectionWcfApp/1.0 (Android app personnelle de collection)")
                    .build()
                chain.proceed(request)
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }
}
