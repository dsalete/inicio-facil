package com.familia.inicio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable

/** Icono de una app instalada y un color de fondo acorde con ese icono. */
object AppIcons {

    private val cache = HashMap<String, Int>()
    private const val FALLBACK = 0xFF455A64.toInt()

    fun icon(ctx: Context, pkg: String): Drawable? {
        return try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }

    /** Color oscuro (para que el texto blanco se lea bien) basado en el tono dominante del icono. */
    fun color(ctx: Context, pkg: String): Int {
        val cached = cache[pkg]
        if (cached != null) return cached
        val result = try {
            compute(ctx, pkg)
        } catch (e: Exception) {
            FALLBACK
        }
        cache[pkg] = result
        return result
    }

    private fun compute(ctx: Context, pkg: String): Int {
        val d = icon(ctx, pkg) ?: return FALLBACK
        val size = 32
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, size, size)
        d.draw(Canvas(bmp))

        // 12 grupos de tono; gana el que más color vivo tenga.
        val weight = DoubleArray(12)
        val sumR = DoubleArray(12)
        val sumG = DoubleArray(12)
        val sumB = DoubleArray(12)
        val hsv = FloatArray(3)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val p = bmp.getPixel(x, y)
                val a = Color.alpha(p) / 255.0
                if (a < 0.5) continue
                Color.colorToHSV(p, hsv)
                val w = a * hsv[1] * hsv[2]
                if (w < 0.08) continue // ignora blancos, grises y negros
                val bucket = ((hsv[0] / 30f).toInt()).coerceIn(0, 11)
                weight[bucket] += w
                sumR[bucket] += Color.red(p) * w
                sumG[bucket] += Color.green(p) * w
                sumB[bucket] += Color.blue(p) * w
            }
        }
        var best = 0
        for (i in 1 until 12) {
            if (weight[i] > weight[best]) best = i
        }
        if (weight[best] <= 0.0) return FALLBACK

        val r = (sumR[best] / weight[best]).toInt()
        val g = (sumG[best] / weight[best]).toInt()
        val b = (sumB[best] / weight[best]).toInt()
        Color.RGBToHSV(r, g, b, hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.45f)
        hsv[2] = hsv[2].coerceAtMost(0.62f)
        return Color.HSVToColor(hsv)
    }
}
