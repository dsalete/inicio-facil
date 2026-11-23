package com.familia.inicio

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

private val DAYS = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
private val MONTHS = arrayOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
)

/** Botones por página en la pantalla de inicio. */
private const val PER_PAGE = 8

class MainActivity : Activity() {

    private var lastTap = 0L
    private var page = 0
    private val handler = Handler(Looper.getMainLooper())
    private var timeView: TextView? = null
    private var dateView: TextView? = null
    private var resumed = false

    private val ticker = object : Runnable {
        override fun run() {
            updateClock()
            refreshWeather()
            handler.postDelayed(this, 1000)
        }
    }

    // Pide el tiempo si toca (cada 30 minutos); al llegar, redibuja la tarjeta.
    private fun refreshWeather() {
        Weather.refresh(this) { if (resumed && !isFinishing) buildScreen() }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        buildScreen()
        handler.removeCallbacks(ticker)
        ticker.run()
        Updater.checkIfDue(this)
        GameIcon.refresh(this) { if (resumed && !isFinishing) buildScreen() }
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        handler.removeCallbacks(ticker)
    }

    // Al salir a otra app, la próxima vez que se vea Inicio empieza en la primera página.
    override fun onStop() {
        super.onStop()
        page = 0
    }

    // Es la pantalla de inicio: el botón Atrás no hace nada.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun updateClock() {
        val cal = Calendar.getInstance()
        timeView?.text = String.format(
            Locale.US, "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
        )
        dateView?.text = "${DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]} " +
            "${cal.get(Calendar.DAY_OF_MONTH)} ${MONTHS[cal.get(Calendar.MONTH)]}"
    }

    private fun navButton(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 22f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        maxLines = 1
        background = ContactUi.rounded(0xFF6D4C41.toInt(), dp(16))
        setOnClickListener { onClick() }
    }

    private fun buildScreen() {
        val scale = when (Prefs.textSize(this)) {
            0 -> 0.85f
            2 -> 1.2f
            else -> 1f
        }
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            fitsSystemWindows = true
        }

        // Hora grande arriba y, debajo, la fecha.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val time = TextView(this).apply {
            textSize = if (landscape) 56f else 84f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#3E2723"))
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        val date = TextView(this).apply {
            textSize = 28f * scale
            setTextColor(Color.parseColor("#5D4037"))
            gravity = Gravity.CENTER
        }
        header.addView(time)
        header.addView(date)
        timeView = time
        dateView = date
        updateClock()
        root.addView(header, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            setMargins(0, dp(8), 0, dp(10))
        })

        // Botones: 2 columnas en vertical (4 en horizontal), 8 por página.
        // Ajustes es siempre el último de todos.
        val cols = if (landscape) 4 else 2
        val rows = PER_PAGE / cols
        val tiles = Tiles.ordered(this).filter { Prefs.visible(this, it.id) } + Tiles.settings
        val pages = maxOf(1, (tiles.size + PER_PAGE - 1) / PER_PAGE)
        if (page >= pages) page = pages - 1
        if (page < 0) page = 0
        val items = tiles.drop(page * PER_PAGE).take(PER_PAGE)
        // Con una sola página, las filas se reparten el alto; con varias, todas miden igual.
        val rowsUsed = if (pages == 1) maxOf(1, (items.size + cols - 1) / cols) else rows

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (r in 0 until rowsUsed) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0 until cols) {
                val idx = r * cols + col
                if (idx < items.size) {
                    row.addView(
                        tileView(items[idx], scale),
                        LinearLayout.LayoutParams(0, MATCH, 1f).apply {
                            setMargins(dp(6), dp(6), dp(6), dp(6))
                        }
                    )
                } else {
                    row.addView(View(this), LinearLayout.LayoutParams(0, MATCH, 1f))
                }
            }
            grid.addView(row, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
        root.addView(grid, LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
            setMargins(dp(6), 0, dp(6), dp(6))
        })

        // Anterior / página / Siguiente (solo si hay más de una página).
        if (pages > 1) {
            val nav = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val prev = navButton("Anterior") {
                page--
                buildScreen()
            }
            val next = navButton("Siguiente") {
                page++
                buildScreen()
            }
            prev.visibility = if (page > 0) View.VISIBLE else View.INVISIBLE
            next.visibility = if (page < pages - 1) View.VISIBLE else View.INVISIBLE
            nav.addView(prev, LinearLayout.LayoutParams(0, dp(60), 1f))
            nav.addView(TextView(this).apply {
                text = "${page + 1} de $pages"
                textSize = 18f
                setTextColor(Color.parseColor("#5D4037"))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(80), WRAP))
            nav.addView(next, LinearLayout.LayoutParams(0, dp(60), 1f))
            root.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(12), dp(2), dp(12), dp(12))
            })
        }

        setContentView(root)
    }

    /** ¿El icono ya trae fondo propio (adaptativo, o imagen que llena el cuadro)? */
    private fun hasOwnBackground(d: Drawable): Boolean {
        if (d is AdaptiveIconDrawable) return true
        if (d is BitmapDrawable) {
            try {
                return bitmapFillsSquare(d)
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    private fun bitmapFillsSquare(d: BitmapDrawable): Boolean {
        val b = d.bitmap ?: return false
        val w = b.width
        val h = b.height
        if (w < 8 || h < 8) return false
        val pts = arrayOf(
            intArrayOf(w / 2, 3), intArrayOf(w / 2, h - 4),
            intArrayOf(3, h / 2), intArrayOf(w - 4, h / 2)
        )
        var opaque = 0
        for (p in pts) {
            if (Color.alpha(b.getPixel(p[0], p[1])) > 200) opaque++
        }
        return opaque >= 3
    }

    /** Un botón: icono grande (el original de la app) arriba y el nombre debajo, siempre visibles. */
    private fun tileView(t: Tile, scale: Float): View {
        if (t.id == "tiempo") return weatherTile(t, scale)
        val bg = ContactUi.rounded(t.color, dp(24))
        val ripple = RippleDrawable(
            ColorStateList.valueOf(Color.argb(90, 255, 255, 255)), bg, null
        )
        val icon: Drawable? = try {
            t.iconOf?.invoke(this)
        } catch (e: Exception) {
            null
        }

        val tile = LinearLayout(this)
        tile.orientation = LinearLayout.VERTICAL
        tile.gravity = Gravity.CENTER_HORIZONTAL
        tile.background = ripple
        tile.elevation = dp(4).toFloat()
        tile.setPadding(dp(8), dp(10), dp(8), dp(8))

        // Zona del icono: ocupa el espacio que deja el nombre.
        val box = FrameLayout(this)
        if (icon != null) {
            val iv = SquareImageView(this)
            iv.setImageDrawable(icon)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            // Si el icono ya trae su fondo, se muestra tal cual; si es transparente, va sobre un cuadro blanco.
            if (!hasOwnBackground(icon)) {
                iv.background = ContactUi.rounded(Color.argb(235, 255, 255, 255), dp(18))
                iv.setPadding(dp(6), dp(6), dp(6), dp(6))
            }
            box.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
        } else {
            val tv = TextView(this)
            tv.text = t.emoji
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER
            tv.setAutoSizeTextTypeUniformWithConfiguration(20, 80, 2, TypedValue.COMPLEX_UNIT_SP)
            box.addView(tv, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
        }
        tile.addView(box, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val label = TextView(this)
        label.text = t.label
        label.textSize = 22f * scale
        label.setTypeface(label.typeface, Typeface.BOLD)
        label.setTextColor(Color.WHITE)
        label.gravity = Gravity.CENTER
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        label.setPadding(0, dp(6), 0, 0)
        tile.addView(label, LinearLayout.LayoutParams(MATCH, WRAP))

        tile.setOnClickListener { open(t) }
        return tile
    }

    /** Tarjeta del tiempo: emoji del estado, temperatura grande y el nombre del estado. */
    private fun weatherTile(t: Tile, scale: Float): View {
        val w = Weather.cached(this)
        val emoji: String
        val name: String
        if (w != null) {
            val info = Weather.info(w.code, w.isDay)
            emoji = info.first
            name = info.second
        } else {
            emoji = t.emoji
            name = if (Weather.hasPermission(this)) "Tiempo" else "Activar"
        }
        val bg = ContactUi.rounded(t.color, dp(24))
        val ripple = RippleDrawable(
            ColorStateList.valueOf(Color.argb(90, 255, 255, 255)), bg, null
        )
        val tile = LinearLayout(this)
        tile.orientation = LinearLayout.VERTICAL
        tile.gravity = Gravity.CENTER_HORIZONTAL
        tile.background = ripple
        tile.elevation = dp(4).toFloat()
        tile.setPadding(dp(8), dp(10), dp(8), dp(8))

        val emo = TextView(this)
        emo.text = emoji
        emo.setTextColor(Color.WHITE)
        emo.gravity = Gravity.CENTER
        emo.setAutoSizeTextTypeUniformWithConfiguration(20, 80, 2, TypedValue.COMPLEX_UNIT_SP)
        tile.addView(emo, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val temp = TextView(this)
        temp.text = if (w != null) "${w.temp}°" else "--"
        temp.textSize = 40f * scale
        temp.setTypeface(temp.typeface, Typeface.BOLD)
        temp.setTextColor(Color.WHITE)
        temp.gravity = Gravity.CENTER
        temp.maxLines = 1
        temp.includeFontPadding = false
        tile.addView(temp, LinearLayout.LayoutParams(MATCH, WRAP))

        val label = TextView(this)
        label.text = name
        label.textSize = 22f * scale
        label.setTypeface(label.typeface, Typeface.BOLD)
        label.setTextColor(Color.WHITE)
        label.gravity = Gravity.CENTER
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        label.setPadding(0, dp(4), 0, 0)
        tile.addView(label, LinearLayout.LayoutParams(MATCH, WRAP))

        tile.setOnClickListener { open(t) }
        return tile
    }

    private fun open(t: Tile) {
        // Ignora toques repetidos (por temblor o doble toque sin querer).
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < Prefs.debounceMs(this)) return
        lastTap = now

        // Ajustes pide el PIN para evitar entrar por error.
        if (t.id == Tiles.settings.id) {
            askPin()
            return
        }

        val intent = try {
            t.intent(this)
        } catch (e: Exception) {
            null
        }
        if (intent == null) {
            Toast.makeText(this, "${t.label} no está instalada en este móvil", Toast.LENGTH_LONG).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se puede abrir ${t.label}", Toast.LENGTH_LONG).show()
        }
    }

    private fun askPin() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
        }
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage("Introduce el PIN")
            .setView(input)
            .setPositiveButton("Entrar") { _, _ ->
                if (input.text.toString() == Prefs.pin(this)) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
