package com.familia.inicio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore

/**
 * Un botón de la pantalla de inicio.
 * [pkg] solo se usa en las apps añadidas desde Ajustes (se muestra su icono en vez del emoji).
 * [intent] devuelve lo que hay que abrir, o null si la app no está instalada.
 */
class Tile(
    val id: String,
    val label: String,
    val emoji: String,
    val color: Int,
    val pkg: String? = null,
    val intent: (Context) -> Intent?
)

object Tiles {

    /** Abre la primera app instalada de la lista de paquetes. */
    private fun app(c: Context, vararg packages: String): Intent? {
        for (p in packages) {
            val i = c.packageManager.getLaunchIntentForPackage(p)
            if (i != null) return i
        }
        return null
    }

    // Botones de serie, en su orden por defecto.
    val all: List<Tile> = listOf(
        Tile("camara", "Cámara", "📷", 0xFF37474F.toInt()) {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        },
        Tile("fotos", "Fotos", "🖼️", 0xFF1565C0.toInt()) { c ->
            app(c, "com.google.android.apps.photos")
        },
        Tile("whatsapp", "WhatsApp", "💬", 0xFF2E7D32.toInt()) { c ->
            app(c, "com.whatsapp")
        },
        Tile("juegos", "Juegos", "🧩", 0xFF6A1B9A.toInt()) { c ->
            Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.gamesUrl(c)))
        },
        Tile("llamar", "Llamar", "📞", 0xFF00796B.toInt()) { c ->
            Intent(c, ContactsActivity::class.java)
        },
        Tile("youtube", "YouTube", "▶️", 0xFFC62828.toInt()) { c ->
            app(c, "com.google.android.youtube")
        },
        Tile("tiktok", "TikTok", "🎵", 0xFF212121.toInt()) { c ->
            app(c, "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        },
        Tile("alarmas", "Alarmas", "⏰", 0xFFE65100.toInt()) {
            Intent(AlarmClock.ACTION_SHOW_ALARMS)
        }
    )

    /** Apps añadidas desde Ajustes (solo las que siguen instaladas). */
    fun custom(c: Context): List<Tile> {
        return Prefs.customApps(c)
            .filter { c.packageManager.getLaunchIntentForPackage(it.pkg) != null }
            .map { a ->
                Tile("app:${a.pkg}", a.label, "", AppIcons.color(c, a.pkg), a.pkg) { ctx ->
                    ctx.packageManager.getLaunchIntentForPackage(a.pkg)
                }
            }
    }

    /** Todos los botones en el orden elegido en ajustes (los nuevos se añaden al final). */
    fun ordered(c: Context): List<Tile> {
        val every = all + custom(c)
        val byId = every.associateBy { it.id }
        val result = Prefs.tileOrder(c).mapNotNull { byId[it] }.toMutableList()
        for (t in every) {
            if (!result.contains(t)) result.add(t)
        }
        return result
    }
}
