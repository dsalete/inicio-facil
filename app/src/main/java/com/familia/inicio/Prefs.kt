package com.familia.inicio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Un contacto elegido para la pantalla «Llamar». La foto se lee de la agenda al mostrarlo. */
class Contact(val id: Long, val name: String, val number: String)

/** Una app instalada que se ha añadido a la pantalla de inicio desde Ajustes. */
class CustomApp(val pkg: String, val label: String)

/** Ajustes guardados en el propio móvil. */
object Prefs {
    const val DEFAULT_GAMES_URL = "https://juegos-faciles.netlify.app"

    private fun sp(c: Context) = c.getSharedPreferences("inicio", Context.MODE_PRIVATE)

    fun pin(c: Context): String = sp(c).getString("pin", "1234") ?: "1234"
    fun setPin(c: Context, v: String) = sp(c).edit().putString("pin", v).apply()

    fun visible(c: Context, id: String): Boolean = sp(c).getBoolean("show_$id", true)
    fun setVisible(c: Context, id: String, v: Boolean) =
        sp(c).edit().putBoolean("show_$id", v).apply()

    /** 0 = pequeño, 1 = normal, 2 = grande */
    fun textSize(c: Context): Int = sp(c).getInt("text_size", 1)
    fun setTextSize(c: Context, v: Int) = sp(c).edit().putInt("text_size", v).apply()

    /** Milisegundos durante los que se ignoran toques repetidos. */
    fun debounceMs(c: Context): Long = sp(c).getLong("debounce_ms", 1000L)
    fun setDebounceMs(c: Context, v: Long) = sp(c).edit().putLong("debounce_ms", v).apply()

    fun gamesUrl(c: Context): String =
        sp(c).getString("games_url", DEFAULT_GAMES_URL) ?: DEFAULT_GAMES_URL
    fun setGamesUrl(c: Context, v: String) = sp(c).edit().putString("games_url", v).apply()

    /** Orden de los botones de la pantalla de inicio (lista de ids). */
    fun tileOrder(c: Context): List<String> {
        val s = sp(c).getString("tile_order", "") ?: ""
        return if (s.isEmpty()) emptyList() else s.split(",")
    }

    fun setTileOrder(c: Context, ids: List<String>) =
        sp(c).edit().putString("tile_order", ids.joinToString(",")).apply()

    /** Apps añadidas a la pantalla de inicio desde Ajustes. */
    fun customApps(c: Context): MutableList<CustomApp> {
        val out = mutableListOf<CustomApp>()
        try {
            val arr = JSONArray(sp(c).getString("custom_apps", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(CustomApp(o.getString("pkg"), o.getString("label")))
            }
        } catch (e: Exception) {
            // Si algo falla al leer, se empieza con la lista vacía.
        }
        return out
    }

    fun setCustomApps(c: Context, list: List<CustomApp>) {
        val arr = JSONArray()
        for (a in list) {
            arr.put(JSONObject().put("pkg", a.pkg).put("label", a.label))
        }
        sp(c).edit().putString("custom_apps", arr.toString()).apply()
    }

    /** Contactos de la pantalla «Llamar», en el orden en que se muestran. */
    fun contacts(c: Context): MutableList<Contact> {
        val out = mutableListOf<Contact>()
        try {
            val arr = JSONArray(sp(c).getString("contacts", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Contact(o.getLong("id"), o.getString("name"), o.getString("number")))
            }
        } catch (e: Exception) {
            // Si algo falla al leer, se empieza con la lista vacía.
        }
        return out
    }

    fun setContacts(c: Context, list: List<Contact>) {
        val arr = JSONArray()
        for (k in list) {
            arr.put(JSONObject().put("id", k.id).put("name", k.name).put("number", k.number))
        }
        sp(c).edit().putString("contacts", arr.toString()).apply()
    }
}
