package com.familia.inicio

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar
import java.util.Locale

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Pantalla del tiempo: ahora, hoy y mañana, en letra grande. */
class WeatherActivity : Activity() {

    private val reqLocation = 44
    private lateinit var box: LinearLayout

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#E3F2FD"))
        scroll.fitsSystemWindows = true
        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(16), dp(20), dp(20))
        scroll.addView(box, ViewGroup.LayoutParams(MATCH, WRAP))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
        if (!Weather.hasPermission(this)) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), reqLocation)
        } else {
            Weather.refresh(this, true) { if (!isFinishing) render() }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Weather.hasPermission(this)) {
            Weather.refresh(this, true) { if (!isFinishing) render() }
        }
        render()
    }

    private fun text(s: String, size: Float, bold: Boolean = false, color: String = "#0D47A1") =
        TextView(this).apply {
            text = s
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun dayCard(title: String, d: WeatherDay) {
        val (emoji, name) = Weather.info(d.code, true)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(dp(12), dp(14), dp(12), dp(14))
        card.background = ContactUi.rounded(Color.WHITE, dp(20))
        card.addView(text(title, 22f, true))
        card.addView(text("$emoji  $name", 28f))
        card.addView(text("Máxima ${d.max}°   Mínima ${d.min}°", 22f))
        if (d.rain > 0) card.addView(text("Probabilidad de lluvia: ${d.rain}%", 20f))
        box.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
    }

    private fun render() {
        box.removeAllViews()
        val w = Weather.cached(this)
        if (w == null) {
            box.addView(text("🌤️", 80f))
            if (Weather.hasPermission(this)) {
                box.addView(text("Buscando el tiempo…", 28f, true))
                box.addView(text("Hace falta tener internet.", 22f))
            } else {
                box.addView(text("Para ver el tiempo hace falta permitir la ubicación.", 24f, true))
                box.addView(Button(this).apply {
                    text = "Permitir ubicación"
                    textSize = 22f
                    minimumHeight = dp(64)
                    setOnClickListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), reqLocation
                        )
                    }
                }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })
            }
        } else {
            val (emoji, name) = Weather.info(w.code, w.isDay)
            if (w.place.isNotEmpty()) box.addView(text(w.place, 26f, true))
            box.addView(text(emoji, 96f))
            box.addView(text("${w.temp}°", 72f, true))
            box.addView(text(name, 32f))
            dayCard("Hoy", w.today)
            if (w.tomorrow != null) dayCard("Mañana", w.tomorrow)
            val cal = Calendar.getInstance()
            cal.timeInMillis = w.time
            box.addView(
                text(
                    String.format(
                        Locale.US, "Actualizado a las %02d:%02d",
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
                    ), 16f, false, "#546E7A"
                ),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
            )
        }
        box.addView(Button(this).apply {
            text = "Volver"
            textSize = 24f
            minimumHeight = dp(72)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(20) })
    }
}
