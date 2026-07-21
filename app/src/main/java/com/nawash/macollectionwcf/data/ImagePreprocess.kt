package com.nawash.macollectionwcf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri

/**
 * Prétraitement local léger de la photo avant l'analyse OCR, sans dépendance native lourde type
 * OpenCV. Réduit la taille, puis rehausse contraste + luminosité pour rendre le texte (souvent
 * japonais, sur les boîtes WCF) plus lisible. La couleur est conservée.
 */
object ImagePreprocess {

    fun enhance(context: Context, uri: Uri, maxSize: Int = 1600): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longest / sample > maxSize) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val c = 1.2f
        val t = (-0.5f * c + 0.5f) * 255f + 8f
        val matrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        if (out != src) src.recycle()
        out
    } catch (e: Exception) {
        null
    }
}
