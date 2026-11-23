package com.familia.inicio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.telecom.TelecomManager

/**
 * Un botón de la pantalla de inicio.
 * [pkg] solo se usa en las apps añadidas desde Ajustes.
 * [iconOf] devuelve el icono original de la app (o null para usar el emoji).
 * [intent] devuelve lo que hay que abrir, o null si la app no está instalada.
 */
class Tile(
    val id: String,
    val label: String,
    val emoji: String,
    val color: Int,
    val pkg: String? = null,
    val iconOf: ((Context) -> Drawable?)? = null,
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

    /** Icono de la primera app instalada de la lista de paquetes. */
    private fun appIcon(c: Context, vararg packages: String): Drawable? {
        for (p in packages) {
            val d = AppIcons.icon(c, p)
            if (d != null) return d
        }
        return null
    }

    /** Icono de la app que atiende una acción (por ejemplo, la cámara). */
    private fun handlerIcon(c: Context, intent: Intent): Drawable? {
        return try {
            val pm = c.packageManager
            var r = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (r == null || r.activityInfo.packageName == "android") {
                r = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()
            }
            r?.loadIcon(pm)
        } catch (e: Exception) {
            null
        }
    }

    private fun dialerIcon(c: Context): Drawable? {
        try {
            val tm = c.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val p = tm.defaultDialerPackage
            if (p != null) {
                val d = AppIcons.icon(c, p)
                if (d != null) return d
            }
        } catch (e: Exception) {
            // Se prueba el siguiente método.
        }
        return handlerIcon(c, Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
    }

    // --- App de reloj/alarmas: cada fabricante usa un paquete distinto ---
    private val clockPackages = listOf(
        "com.google.android.deskclock",
        "com.coloros.alarmclock",
        "com.oplus.alarmclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.oneplus.deskclock",
        "com.huawei.deskclock",
        "com.miui.clock"
    )
    private var clockSearched = false
    private var clockFound: String? = null

    /** Paquete de la app de reloj del móvil, o null si no se encuentra. */
    private fun clockPackage(c: Context): String? {
        if (clockSearched) return clockFound
        val pm = c.packageManager
        var found: String? = null
        for (p in clockPackages) {
            if (pm.getLaunchIntentForPackage(p) != null) {
                found = p
                break
            }
        }
        if (found == null) {
            val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            for (ri in pm.queryIntentActivities(main, 0)) {
                val p = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString().lowercase()
                if (p.lowercase().contains("clock") || label.contains("reloj") ||
                    label.contains("clock") || label.contains("alarm")
                ) {
                    found = p
                    break
                }
            }
        }
        clockFound = found
        clockSearched = true
        return found
    }

    // Botones de serie, en su orden por defecto.
    val all: List<Tile> = listOf(
        Tile("tiempo", "Tiempo", "🌤️", 0xFF0277BD.toInt()) { c ->
            Intent(c, WeatherActivity::class.java)
        },
        Tile("camara", "Cámara", "📷", 0xFF37474F.toInt(),
            iconOf = { c -> handlerIcon(c, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
        ) {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        },
        Tile("fotos", "Fotos", "🖼️", 0xFF1565C0.toInt(),
            iconOf = { c -> appIcon(c, "com.google.android.apps.photos") }
        ) { c ->
            app(c, "com.google.android.apps.photos")
        },
        Tile("whatsapp", "WhatsApp", "💬", 0xFF2E7D32.toInt(),
            iconOf = { c -> appIcon(c, "com.whatsapp") }
        ) { c ->
            app(c, "com.whatsapp")
        },
        Tile("juegos", "Juegos", "🧩", 0xFF6A1B9A.toInt(),
            iconOf = { c -> GameIcon.of(c) }
        ) { c ->
            Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.gamesUrl(c)))
        },
        Tile("llamar", "Llamar", "📞", 0xFF00796B.toInt(),
            iconOf = { c -> dialerIcon(c) }
        ) { c ->
            Intent(c, ContactsActivity::class.java)
        },
        Tile("youtube", "YouTube", "▶️", 0xFFC62828.toInt(),
            iconOf = { c -> appIcon(c, "com.google.android.youtube") }
        ) { c ->
            app(c, "com.google.android.youtube")
        },
        Tile("tiktok", "TikTok", "🎵", 0xFF212121.toInt(),
            iconOf = { c -> appIcon(c, "com.zhiliaoapp.musically", "com.ss.android.ugc.trill") }
        ) { c ->
            app(c, "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        },
        Tile("alarmas", "Alarmas", "⏰", 0xFFE65100.toInt(),
            iconOf = { c ->
                val p = clockPackage(c)
                if (p != null) AppIcons.icon(c, p) else null
            }
        ) { c ->
            val p = clockPackage(c)
            val launch = if (p != null) c.packageManager.getLaunchIntentForPackage(p) else null
            launch ?: Intent(AlarmClock.ACTION_SHOW_ALARMS)
        }
    )

    /** Botón de Ajustes: siempre el último de todos y no se puede ocultar ni mover. */
    val settings = Tile("ajustes", "Ajustes", "⚙️", 0xFF546E7A.toInt()) { null }

    /** Apps añadidas desde Ajustes (solo las que siguen instaladas). */
    fun custom(c: Context): List<Tile> {
        return Prefs.customApps(c)
            .filter { c.packageManager.getLaunchIntentForPackage(it.pkg) != null }
            .map { a ->
                Tile("app:${a.pkg}", a.label, "", AppIcons.color(c, a.pkg), a.pkg,
                    iconOf = { ctx -> AppIcons.icon(ctx, a.pkg) }
                ) { ctx ->
                    ctx.packageManager.getLaunchIntentForPackage(a.pkg)
                }
            }
    }

    /** Todos los botones (sin Ajustes) en el orden elegido; los nuevos se añaden al final. */
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
