package com.familia.inicio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Previsión de un día. */
class WeatherDay(val code: Int, val max: Int, val min: Int, val rain: Int)

/** Tiempo actual y previsión de hoy y mañana. [time] es la hora (ms) de la última consulta. */
class WeatherData(
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val today: WeatherDay,
    val tomorrow: WeatherDay?,
    val place: String,
    val time: Long
)

/**
 * Tiempo con Open-Meteo (gratis, sin clave) y la ubicación aproximada del móvil.
 * El último dato se guarda para enseñarlo aunque no haya internet.
 */
object Weather {

    private const val EVERY_MS = 30L * 60 * 1000
    private const val RETRY_MS = 3L * 60 * 1000

    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var lastAttempt = 0L
    private var lastFailed = false

    fun hasPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Emoji y nombre corto del estado del tiempo (códigos WMO de Open-Meteo). */
    fun info(code: Int, isDay: Boolean = true): Pair<String, String> = when (code) {
        0 -> if (isDay) Pair("☀️", "Despejado") else Pair("🌙", "Despejado")
        1 -> if (isDay) Pair("🌤️", "Poco nuboso") else Pair("🌙", "Poco nuboso")
        2 -> Pair("⛅", "Nuboso")
        3 -> Pair("☁️", "Nublado")
        45, 48 -> Pair("🌫️", "Niebla")
        51, 53, 55, 56, 57 -> Pair("🌦️", "Llovizna")
        61, 63, 65, 66, 67 -> Pair("🌧️", "Lluvia")
        71, 73, 75, 77, 85, 86 -> Pair("🌨️", "Nieve")
        80, 81, 82 -> Pair("🌧️", "Chubascos")
        95, 96, 99 -> Pair("⛈️", "Tormenta")
        else -> Pair("🌡️", "Tiempo")
    }

    /** Último dato guardado, o null si todavía no hay ninguno. */
    fun cached(ctx: Context): WeatherData? {
        val s = Prefs.weather(ctx) ?: return null
        return try {
            val j = JSONObject(s)
            fun day(o: JSONObject) = WeatherDay(
                o.getInt("code"), o.getInt("max"), o.getInt("min"), o.optInt("rain", 0)
            )
            WeatherData(
                j.getInt("temp"), j.getInt("code"), j.optBoolean("day", true),
                day(j.getJSONObject("d0")),
                if (j.has("d1")) day(j.getJSONObject("d1")) else null,
                j.optString("place", ""), j.optLong("time", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Actualiza el tiempo si toca (cada 30 minutos; si falló, cada 3). [force] lo pide ya.
     * [done] se llama en el hilo principal cuando hay datos nuevos.
     */
    fun refresh(ctx: Context, force: Boolean = false, done: () -> Unit) {
        if (running) return
        val app = ctx.applicationContext
        val now = System.currentTimeMillis()
        val wait = if (lastFailed) RETRY_MS else EVERY_MS
        if (!force && now - lastAttempt < wait) return
        val knownPlace = Prefs.lat(app) != null
        if (!hasPermission(app) && !knownPlace) return
        running = true
        lastAttempt = now
        locate(app) { loc ->
            var lat = loc?.latitude
            var lon = loc?.longitude
            if (lat != null && lon != null) {
                Prefs.setLatLon(app, lat, lon)
            } else {
                lat = Prefs.lat(app)
                lon = Prefs.lon(app)
            }
            if (lat == null || lon == null) {
                finish(false, done)
                return@locate
            }
            val la: Double = lat
            val lo: Double = lon
            Thread {
                val ok = try {
                    fetch(app, la, lo)
                    true
                } catch (e: Exception) {
                    false
                }
                main.post { finish(ok, done) }
            }.start()
        }
    }

    private fun finish(ok: Boolean, done: () -> Unit) {
        running = false
        lastFailed = !ok
        if (ok) done()
    }

    /** Posición aproximada: la última conocida o, si es antigua, una nueva por red. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun locate(ctx: Context, result: (Location?) -> Unit) {
        if (!hasPermission(ctx)) {
            result(null)
            return
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        try {
            for (p in lm.getProviders(true)) {
                try {
                    val l = lm.getLastKnownLocation(p)
                    val cur = best
                    if (l != null && (cur == null || l.time > cur.time)) best = l
                } catch (e: Exception) {
                    // Se prueba con el siguiente proveedor.
                }
            }
        } catch (e: Exception) {
            // Sin proveedores: se usa lo guardado.
        }
        val known = best
        val fresh = known != null &&
            System.currentTimeMillis() - known.time < 6L * 60 * 60 * 1000
        if (fresh) {
            result(known)
            return
        }
        val canAsk = try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
        if (!canAsk) {
            result(known)
            return
        }
        var answered = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (answered) return
                answered = true
                result(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            lm.requestSingleUpdate(
                LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            result(known)
            return
        }
        // Si no llega en 15 segundos, se sigue con lo que haya.
        main.postDelayed({
            if (!answered) {
                answered = true
                try {
                    lm.removeUpdates(listener)
                } catch (e: Exception) {
                    // Nada que quitar.
                }
                result(known)
            }
        }, 15000)
    }

    /** Descarga el tiempo (hilo secundario) y lo guarda. Lanza excepción si falla. */
    @Suppress("DEPRECATION")
    private fun fetch(ctx: Context, lat: Double, lon: Double) {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=2",
            lat, lon
        )
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "InicioFacil")
        val body = try {
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val j = JSONObject(body)
        val cur = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        fun day(i: Int): JSONObject = JSONObject()
            .put("code", d.getJSONArray("weather_code").getInt(i))
            .put("max", Math.round(d.getJSONArray("temperature_2m_max").getDouble(i)).toInt())
            .put("min", Math.round(d.getJSONArray("temperature_2m_min").getDouble(i)).toInt())
            .put("rain", d.getJSONArray("precipitation_probability_max").optInt(i, 0))

        var place = cached(ctx)?.place ?: ""
        try {
            val list = Geocoder(ctx, Locale("es")).getFromLocation(lat, lon, 1)
            val a = list?.firstOrNull()
            val name = a?.locality ?: a?.subAdminArea
            if (!name.isNullOrBlank()) place = name
        } catch (e: Exception) {
            // Sin nombre de lugar: se enseña solo el tiempo.
        }

        val out = JSONObject()
            .put("temp", Math.round(cur.getDouble("temperature_2m")).toInt())
            .put("code", cur.getInt("weather_code"))
            .put("day", cur.optInt("is_day", 1) == 1)
            .put("d0", day(0))
            .put("place", place)
            .put("time", System.currentTimeMillis())
        if (d.getJSONArray("weather_code").length() > 1) out.put("d1", day(1))
        Prefs.setWeather(ctx, out.toString())
    }
}
