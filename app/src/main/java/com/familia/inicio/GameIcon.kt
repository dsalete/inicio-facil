package com.familia.inicio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Icono del botón Juegos: el de la propia web de juegos.
 * 1) Si la web está instalada como app en el móvil, usa su icono.
 * 2) Si no, descarga el icono que declara la web (manifest) y lo guarda.
 */
object GameIcon {

    private const val EVERY_MS = 24L * 60 * 60 * 1000
    private val main = Handler(Looper.getMainLooper())
    private var running = false

    private fun file(c: Context) = File(c.filesDir, "games_icon.png")

    /** Icono de la web de juegos instalada como app (WebAPK), si existe. */
    private fun installed(c: Context): Drawable? {
        return try {
            val pm = c.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val p = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString().lowercase()
                if (p.contains("webapk") && label.contains("juegos")) return ri.loadIcon(pm)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun cached(c: Context): Drawable? {
        val f = file(c)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(f.path) ?: return null
        return BitmapDrawable(c.resources, bmp)
    }

    fun of(c: Context): Drawable? = installed(c) ?: cached(c)

    /** Descarga el icono si no hay o ha cambiado la dirección (máximo una vez al día). */
    fun refresh(ctx: Context, done: () -> Unit) {
        if (running) return
        val app = ctx.applicationContext
        val url = Prefs.gamesUrl(app)
        val last = Prefs.gameIconStamp(app)
        val key = "$url|"
        val fresh = file(app).exists() && last.startsWith(key) &&
            System.currentTimeMillis() - (last.substringAfter('|').toLongOrNull() ?: 0L) < EVERY_MS
        if (fresh) return
        running = true
        Thread {
            val ok = try {
                download(app, url)
            } catch (e: Exception) {
                false
            }
            main.post {
                running = false
                // Se apunta el intento aunque falle, para no repetirlo sin parar.
                Prefs.setGameIconStamp(app, key + System.currentTimeMillis())
                if (ok) done()
            }
        }.start()
    }

    private fun get(url: String): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "InicioFacil")
        return try {
            if (conn.responseCode != 200) null else conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun tagWith(html: String, rel: String): String? {
        val re = Regex("<link[^>]*rel=[\"'][^\"']*$rel[^\"']*[\"'][^>]*>", RegexOption.IGNORE_CASE)
        return re.find(html)?.value
    }

    private fun href(tag: String): String? =
        Regex("href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)

    private fun download(ctx: Context, base: String): Boolean {
        val html = String(get(base) ?: return false)
        var iconUrl: String? = null

        // Primero el manifest de la web: se elige el icono más grande.
        val manifestHref = tagWith(html, "manifest")?.let { href(it) }
        if (manifestHref != null) {
            val manifestUrl = URL(URL(base), manifestHref)
            val json = JSONObject(String(get(manifestUrl.toString()) ?: return false))
            val icons = json.optJSONArray("icons")
            var bestSize = -1
            if (icons != null) {
                for (i in 0 until icons.length()) {
                    val o = icons.getJSONObject(i)
                    val src = o.optString("src")
                    if (src.isEmpty()) continue
                    val purpose = o.optString("purpose", "any")
                    var size = o.optString("sizes", "0")
                        .substringBefore('x').trim().toIntOrNull() ?: 0
                    if (purpose.contains("maskable") && !purpose.contains("any")) size -= 1000
                    if (size > bestSize) {
                        bestSize = size
                        iconUrl = URL(manifestUrl, src).toString()
                    }
                }
            }
        }
        // Si no hay manifest, el icono de Apple (si lo tiene).
        if (iconUrl == null) {
            val h = tagWith(html, "apple-touch-icon")?.let { href(it) }
            if (h != null) iconUrl = URL(URL(base), h).toString()
        }
        val u = iconUrl ?: return false
        val bytes = get(u) ?: return false
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return true
    }
}
