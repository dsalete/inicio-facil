package com.familia.inicio

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

    private val ticker = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        buildScreen()
        handler.removeCallbacks(ticker)
        ticker.run()
    }

    override fun onPause() {
        super.onPause()
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
        background = ContactUi.rounded(0xFF6D4C41.toInt(), dp(16))
        setOnClickListener { onClick() }
    }

    private fun buildScreen() {
        val scale = when (Prefs.textSize(this)) {
            0 -> 0.85f
            2 -> 1.25f
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
        val cols = if (landscape) 4 else 2
        val rows = PER_PAGE / cols
        val tiles = Tiles.ordered(this).filter { Prefs.visible(this, it.id) }
        val pages = maxOf(1, (tiles.size + PER_PAGE - 1) / PER_PAGE)
        if (page >= pages) page = pages - 1
        if (page < 0) page = 0
        val items = tiles.drop(page * PER_PAGE).take(PER_PAGE)
        // Con una sola página, las filas se reparten el alto; con varias, todas miden igual.
        val rowsUsed = if (pages == 1) maxOf(1, (items.size + cols - 1) / cols) else rows

        // Si hay poco alto, el contenido de los botones se encoge un poco para que quepa.
        val reserved = dp(if (pages > 1) 360 else 290)
        val cellH = (resources.displayMetrics.heightPixels - reserved) / rowsUsed
        val fit = (cellH.toFloat() / (dp(104) * scale)).coerceIn(0.6f, 1f)
        val tileScale = scale * fit

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (tiles.isEmpty()) {
            grid.addView(TextView(this).apply {
                text = "No hay botones activos.\nPulsa Ajustes para activarlos."
                textSize = 22f
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(MATCH, MATCH))
        } else {
            for (r in 0 until rowsUsed) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (col in 0 until cols) {
                    val idx = r * cols + col
                    if (idx < items.size) {
                        row.addView(
                            tileView(items[idx], tileScale),
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
            val prev = navButton("◀ Anterior") {
                page--
                buildScreen()
            }
            val next = navButton("Siguiente ▶") {
                page++
                buildScreen()
            }
            prev.visibility = if (page > 0) View.VISIBLE else View.INVISIBLE
            next.visibility = if (page < pages - 1) View.VISIBLE else View.INVISIBLE
            nav.addView(prev, LinearLayout.LayoutParams(0, dp(56), 1f))
            nav.addView(TextView(this).apply {
                text = "${page + 1} de $pages"
                textSize = 18f
                setTextColor(Color.parseColor("#5D4037"))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(80), WRAP))
            nav.addView(next, LinearLayout.LayoutParams(0, dp(56), 1f))
            root.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(12), dp(2), dp(12), dp(8))
            })
        }

        // Botón de ajustes, abajo del todo (pide PIN).
        val settings = TextView(this).apply {
            text = "⚙️  Ajustes"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#5D4037"))
            gravity = Gravity.CENTER
            background = ContactUi.rounded(Color.parseColor("#E6DCC3"), dp(16))
            setOnClickListener { askPin() }
        }
        root.addView(settings, LinearLayout.LayoutParams(MATCH, dp(56)).apply {
            setMargins(dp(12), dp(2), dp(12), dp(12))
        })

        setContentView(root)
    }

    private fun tileView(t: Tile, scale: Float): View {
        val bg = ContactUi.rounded(t.color, dp(24))
        val ripple = RippleDrawable(
            ColorStateList.valueOf(Color.argb(90, 255, 255, 255)), bg, null
        )
        val pkg = t.pkg
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = ripple
            elevation = dp(4).toFloat()
            if (pkg != null) {
                // App añadida: su icono real.
                val iconSize = dp((56 * scale).toInt())
                addView(ImageView(context).apply {
                    setImageDrawable(AppIcons.icon(context, pkg))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(iconSize, iconSize))
            } else {
                addView(TextView(context).apply {
                    text = t.emoji
                    textSize = 48f * scale
                    gravity = Gravity.CENTER
                })
            }
            addView(TextView(context).apply {
                text = t.label
                textSize = 24f * scale
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(4), dp(6), dp(4), 0)
            })
            setOnClickListener { open(t) }
        }
    }

    private fun open(t: Tile) {
        // Ignora toques repetidos (por temblor o doble toque sin querer).
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < Prefs.debounceMs(this)) return
        lastTap = now

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
