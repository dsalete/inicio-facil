package com.familia.inicio

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Pantalla «Llamar»: fotos de los contactos elegidos, 6 por página. */
class ContactsActivity : Activity() {

    private val perPage = 6
    private var page = 0
    private var lastTap = 0L

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = savedInstanceState?.getInt("page") ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", page)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun button(label: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 22f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = ContactUi.rounded(color, dp(16))
        setOnClickListener { onClick() }
    }

    private fun build() {
        val contacts = Prefs.contacts(this)
        val pages = maxOf(1, (contacts.size + perPage - 1) / perPage)
        if (page >= pages) page = pages - 1
        if (page < 0) page = 0

        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cols = if (landscape) 3 else 2
        val rows = perPage / cols

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            fitsSystemWindows = true
        }

        // Arriba: Volver y título.
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            button("← Volver", 0xFF6D4C41.toInt()) { finish() },
            LinearLayout.LayoutParams(dp(130), dp(52))
        )
        top.addView(TextView(this).apply {
            text = "¿A quién llamas?"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#3E2723"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(top, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            setMargins(dp(10), dp(8), dp(10), dp(6))
        })

        // Centro: cuadrícula de contactos.
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (contacts.isEmpty()) {
            grid.addView(TextView(this).apply {
                text = "No hay contactos elegidos.\nSe añaden en Ajustes."
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#3E2723"))
            }, LinearLayout.LayoutParams(MATCH, MATCH))
        } else {
            val items = contacts.drop(page * perPage).take(perPage)
            val dm = resources.displayMetrics
            val cellW = dm.widthPixels / cols - dp(16)
            val cellH = (dm.heightPixels - dp(250)) / rows - dp(16)
            val avatarSize = minOf(cellW * 0.7f, cellH - dp(56).toFloat())
                .toInt().coerceAtLeast(dp(40))

            for (r in 0 until rows) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (col in 0 until cols) {
                    val idx = r * cols + col
                    if (idx < items.size) {
                        row.addView(
                            cell(items[idx], avatarSize),
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
            setMargins(dp(6), 0, dp(6), 0)
        })

        // Abajo: Anterior / página / Siguiente (solo si hay más de una página).
        if (pages > 1) {
            val nav = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val prev = button("◀ Anterior", 0xFF6D4C41.toInt()) {
                page--
                build()
            }
            val next = button("Siguiente ▶", 0xFF6D4C41.toInt()) {
                page++
                build()
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
                setMargins(dp(10), dp(4), dp(10), dp(12))
            })
        } else {
            root.addView(View(this), LinearLayout.LayoutParams(MATCH, dp(12)))
        }

        setContentView(root)
    }

    private fun cell(k: Contact, avatarSize: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = ContactUi.rounded(Color.WHITE, dp(20))
            elevation = dp(3).toFloat()
            addView(
                ContactUi.avatar(context, k, avatarSize),
                LinearLayout.LayoutParams(avatarSize, avatarSize)
            )
            addView(TextView(context).apply {
                text = k.name
                textSize = 22f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#3E2723"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(6), dp(6), dp(6), 0)
            })
            setOnClickListener { confirm(k) }
        }
    }

    private fun confirm(k: Contact) {
        // Ignora toques repetidos para no abrir dos ventanas.
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < Prefs.debounceMs(this)) return
        lastTap = now

        val size = dp(110)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(20), dp(16), dp(8))
            addView(
                ContactUi.avatar(context, k, size),
                LinearLayout.LayoutParams(size, size)
            )
            addView(TextView(context).apply {
                text = "¿Quieres llamar a ${k.name}?"
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#3E2723"))
                gravity = Gravity.CENTER
                setPadding(0, dp(14), 0, 0)
            })
        }
        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton("Llamar") { _, _ -> call(k) }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 22f
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 20f
    }

    private fun call(k: Contact) {
        val clean = k.number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        val uri = Uri.parse("tel:$clean")
        val granted = checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        // Con permiso llama directamente; sin él, abre el marcador con el número puesto.
        val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
        try {
            startActivity(Intent(action, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "No se puede llamar a ${k.name}", Toast.LENGTH_LONG).show()
        }
    }
}
