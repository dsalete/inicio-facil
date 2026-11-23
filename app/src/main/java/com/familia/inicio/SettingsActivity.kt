package com.familia.inicio

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Collections

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Ajustes solo para ti (se entra con PIN desde la pantalla de inicio). */
class SettingsActivity : Activity() {

    private val reqPick = 42
    private val reqPerm = 43

    // Pantalla que se está viendo: menu, tiles, contacts, display o more.
    private var screen = "menu"
    private var lastScreen = ""

    private lateinit var scroll: ScrollView
    private lateinit var box: LinearLayout
    private var urlInput: EditText? = null

    private val ink = Color.parseColor("#3E2723")
    private val soft = Color.parseColor("#5D4037")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun launch(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            toast("No se puede abrir")
        }
    }

    // ---------- Piezas visuales ----------

    private fun label(s: String, size: Float, bold: Boolean = false, color: Int = ink) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    /** Tarjeta grande de color con emoji, título y una línea de estado. */
    private fun bigCard(
        emoji: String, title: String, sub: String, color: Int, onClick: () -> Unit
    ): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(dp(8), dp(12), dp(8), dp(12))
        card.background = ContactUi.rounded(color, dp(24))
        card.elevation = dp(4).toFloat()
        val e = TextView(this)
        e.text = emoji
        e.textSize = 40f
        e.setTextColor(Color.WHITE)
        e.gravity = Gravity.CENTER
        card.addView(e)
        val t = TextView(this)
        t.text = title
        t.textSize = 20f
        t.setTypeface(t.typeface, Typeface.BOLD)
        t.setTextColor(Color.WHITE)
        t.gravity = Gravity.CENTER
        card.addView(t)
        if (sub.isNotEmpty()) {
            val s = TextView(this)
            s.text = sub
            s.textSize = 14f
            s.setTextColor(Color.argb(225, 255, 255, 255))
            s.gravity = Gravity.CENTER
            card.addView(s)
        }
        card.setOnClickListener { onClick() }
        return card
    }

    private fun addPair(a: View, b: View) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (v in listOf(a, b)) {
            row.addView(v, LinearLayout.LayoutParams(0, MATCH, 1f).apply {
                setMargins(dp(6), dp(6), dp(6), dp(6))
            })
        }
        box.addView(row, LinearLayout.LayoutParams(MATCH, dp(158)))
    }

    /** Botón grande y redondeado de un solo color. */
    private fun bigButton(text: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = ContactUi.rounded(color, dp(20))
        setPadding(dp(12), dp(14), dp(12), dp(14))
        setOnClickListener { onClick() }
    }

    private fun addBig(v: View) {
        box.addView(v, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            setMargins(0, dp(10), 0, dp(4))
        })
    }

    /** Botón redondo pequeño (▲ ▼ ✕). */
    private fun roundButton(text: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = ContactUi.rounded(color, dp(20))
        setOnClickListener { onClick() }
    }

    private fun addRound(row: LinearLayout, b: View) {
        row.addView(b, LinearLayout.LayoutParams(dp(42), dp(42)).apply { leftMargin = dp(6) })
    }

    /** Caja blanca redondeada donde van las filas. */
    private fun panel(): LinearLayout {
        val p = LinearLayout(this)
        p.orientation = LinearLayout.VERTICAL
        p.setPadding(dp(12), dp(8), dp(12), dp(8))
        p.background = ContactUi.rounded(Color.WHITE, dp(20))
        box.addView(p, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        return p
    }

    private fun rowOf(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun divider(p: LinearLayout) {
        p.addView(View(this).apply { setBackgroundColor(Color.parseColor("#ECEFF1")) },
            LinearLayout.LayoutParams(MATCH, dp(1)))
    }

    /** Fila de opciones tipo «pastillas»: la elegida se ve marcada. */
    private fun chips(options: List<String>, selected: Int, sizes: List<Float>? = null,
                      onPick: (Int) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        options.forEachIndexed { i, o ->
            val on = i == selected
            val chip = TextView(this)
            chip.text = o
            chip.textSize = sizes?.get(i) ?: 18f
            chip.setTypeface(chip.typeface, Typeface.BOLD)
            chip.gravity = Gravity.CENTER
            chip.setTextColor(if (on) Color.WHITE else ink)
            chip.background = ContactUi.rounded(
                if (on) Color.parseColor("#6D4C41") else Color.parseColor("#EFEBE9"), dp(16)
            )
            chip.setOnClickListener {
                onPick(i)
                render()
            }
            row.addView(chip, LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                setMargins(dp(3), dp(6), dp(3), dp(6))
            })
        }
        return row
    }

    /** Barra de arriba de las pantallas interiores: Volver + título. */
    private fun backBar(title: String) {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.addView(bigButton("Volver", Color.parseColor("#6D4C41")) { go("menu") },
            LinearLayout.LayoutParams(WRAP, WRAP))
        bar.addView(label(title, 24f, true).apply { gravity = Gravity.END },
            LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(12) })
        box.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    // ---------- Estructura ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = dp(14)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            fitsSystemWindows = true
            addView(box)
        }
        setContentView(scroll)

        askPermissions()
        render()
    }

    override fun onPause() {
        super.onPause()
        saveUrl()
    }

    // Dentro de una sección, Atrás vuelve al menú de Ajustes.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (screen != "menu") {
            go("menu")
        } else {
            super.onBackPressed()
        }
    }

    private fun go(s: String) {
        screen = s
        render()
    }

    private fun saveUrl() {
        val url = urlInput?.text?.toString()?.trim() ?: return
        if (url.startsWith("http://") || url.startsWith("https://")) {
            Prefs.setGamesUrl(this, url)
        }
    }

    private fun missingPermissions(): List<String> = arrayOf(
        Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

    // Permisos: leer contactos (para las fotos), llamar directamente y ubicación (para el tiempo).
    private fun askPermissions() {
        val need = missingPermissions()
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), reqPerm)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }

    /** Dibuja la pantalla actual (se repite al cambiar algo). */
    private fun render() {
        saveUrl()
        urlInput = null
        val y = scroll.scrollY
        box.removeAllViews()
        when (screen) {
            "tiles" -> renderTiles()
            "contacts" -> renderContacts()
            "display" -> renderDisplay()
            "more" -> renderMore()
            else -> renderMenu()
        }
        val keep = if (screen == lastScreen) y else 0
        lastScreen = screen
        scroll.post { scroll.scrollTo(0, keep) }
    }

    // ---------- Menú principal ----------

    private fun renderMenu() {
        box.addView(label("Ajustes", 34f, true))
        box.addView(label("Solo para ti", 16f, false, soft))

        val version = Updater.installedVersion(this)
        val shown = Tiles.ordered(this).count { Prefs.visible(this, it.id) }
        val n = Prefs.contacts(this).size
        val sizeName = listOf("Pequeño", "Normal", "Grande")[Prefs.textSize(this).coerceIn(0, 2)]

        addPair(
            bigCard("🔄", "Actualizar", "Versión $version\nToca para buscar", 0xFF2E7D32.toInt()) {
                Updater.check(this, true)
            },
            bigCard("🏠", "Botones de inicio", "$shown visibles", 0xFF1565C0.toInt()) { go("tiles") }
        )
        addPair(
            bigCard("📞", "Contactos", if (n == 1) "1 contacto" else "$n contactos",
                0xFF00796B.toInt()) { go("contacts") },
            bigCard("🔤", "Texto y toques", "Texto: $sizeName", 0xFF6A1B9A.toInt()) { go("display") }
        )
        addPair(
            bigCard("🔧", "Más opciones", "PIN, juegos, permisos", 0xFF546E7A.toInt()) { go("more") },
            bigCard("🏡", "Volver a Inicio", "", 0xFF6D4C41.toInt()) { finish() }
        )
    }

    // ---------- Botones de inicio ----------

    private fun tileIcon(t: Tile): View {
        val frame = FrameLayout(this)
        frame.background = ContactUi.rounded(t.color, dp(12))
        val icon: Drawable? = try {
            t.iconOf?.invoke(this)
        } catch (e: Exception) {
            null
        }
        if (icon != null) {
            val iv = ImageView(this)
            iv.setImageDrawable(icon)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            iv.setPadding(dp(4), dp(4), dp(4), dp(4))
            frame.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
        } else {
            val tv = TextView(this)
            tv.text = if (t.emoji.isNotEmpty()) t.emoji else t.label.take(1)
            tv.textSize = 24f
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER
            frame.addView(tv, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        return frame
    }

    private fun renderTiles() {
        backBar("Botones de inicio")
        box.addView(label("Activa o apaga cada botón y cámbialos de sitio con ▲ y ▼. " +
            "Ajustes siempre va el último.", 15f, false, soft).apply {
            setPadding(0, dp(10), 0, 0)
        })
        val tiles = Tiles.ordered(this)
        val p = panel()
        tiles.forEachIndexed { i, t ->
            if (i > 0) divider(p)
            val row = rowOf()
            row.addView(tileIcon(t), LinearLayout.LayoutParams(dp(48), dp(48)))
            val name = label(t.label, 18f, true).apply {
                maxLines = 2
                alpha = if (Prefs.visible(this@SettingsActivity, t.id)) 1f else 0.4f
            }
            row.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(10) })
            row.addView(Switch(this).apply {
                isChecked = Prefs.visible(this@SettingsActivity, t.id)
                setOnCheckedChangeListener { _, v ->
                    Prefs.setVisible(this@SettingsActivity, t.id, v)
                    name.alpha = if (v) 1f else 0.4f
                }
            })
            addRound(row, roundButton("▲", Color.parseColor("#78909C")) { moveTile(tiles, i, -1) })
            addRound(row, roundButton("▼", Color.parseColor("#78909C")) { moveTile(tiles, i, 1) })
            if (t.pkg != null) {
                // Las apps añadidas se pueden quitar del todo.
                addRound(row, roundButton("✕", Color.parseColor("#C62828")) {
                    val list = Prefs.customApps(this)
                    list.removeAll { it.pkg == t.pkg }
                    Prefs.setCustomApps(this, list)
                    render()
                })
            }
            p.addView(row)
        }
        addBig(bigButton("＋  Añadir app", 0xFF1565C0.toInt()) { showAppPicker() })
    }

    // ---------- Contactos ----------

    private fun renderContacts() {
        backBar("Contactos")
        box.addView(label("Salen en la pantalla Llamar, 6 por página.", 15f, false, soft).apply {
            setPadding(0, dp(10), 0, 0)
        })
        val contacts = Prefs.contacts(this)
        if (contacts.isEmpty()) {
            val p = panel()
            p.addView(label("Aún no hay contactos. Pulsa «Añadir contacto».", 18f).apply {
                setPadding(0, dp(12), 0, dp(12))
            })
        } else {
            val p = panel()
            contacts.forEachIndexed { i, k ->
                if (i > 0) divider(p)
                val row = rowOf()
                row.addView(ContactUi.avatar(this, k, dp(52)), LinearLayout.LayoutParams(dp(52), dp(52)))
                row.addView(label(ContactUi.pretty(k.name), 18f, true).apply { maxLines = 2 },
                    LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(10) })
                addRound(row, roundButton("▲", Color.parseColor("#78909C")) { moveContact(contacts, i, -1) })
                addRound(row, roundButton("▼", Color.parseColor("#78909C")) { moveContact(contacts, i, 1) })
                addRound(row, roundButton("✕", Color.parseColor("#C62828")) {
                    contacts.removeAt(i)
                    Prefs.setContacts(this, contacts)
                    render()
                })
                p.addView(row)
            }
        }
        addBig(bigButton("＋  Añadir contacto", 0xFF00796B.toInt()) { pickContact() })
    }

    // ---------- Texto y toques ----------

    private fun renderDisplay() {
        backBar("Texto y toques")

        val p1 = panel()
        p1.addView(label("Tamaño del texto", 20f, true))
        p1.addView(label("Los nombres de los botones y la fecha.", 14f, false, soft))
        p1.addView(
            chips(
                listOf("Pequeño", "Normal", "Grande"),
                Prefs.textSize(this).coerceIn(0, 2),
                listOf(15f, 19f, 23f)
            ) { Prefs.setTextSize(this, it) }
        )

        val times = longArrayOf(0L, 500L, 1000L, 2000L)
        val p2 = panel()
        p2.addView(label("Ignorar toques repetidos", 20f, true))
        p2.addView(label("Evita abrir algo dos veces por un toque doble sin querer.", 14f, false, soft))
        p2.addView(
            chips(
                listOf("No", "½ s", "1 s", "2 s"),
                times.indexOf(Prefs.debounceMs(this)).coerceAtLeast(0)
            ) { Prefs.setDebounceMs(this, times[it]) }
        )
    }

    // ---------- Más opciones ----------

    private fun renderMore() {
        backBar("Más opciones")

        val p1 = panel()
        p1.addView(label("Actualizaciones", 20f, true))
        val sw = rowOf()
        sw.addView(label("Avisar en Inicio cuando haya versión nueva", 17f),
            LinearLayout.LayoutParams(0, WRAP, 1f))
        sw.addView(Switch(this).apply {
            isChecked = Prefs.updateAuto(this@SettingsActivity)
            setOnCheckedChangeListener { _, v -> Prefs.setUpdateAuto(this@SettingsActivity, v) }
        })
        p1.addView(sw)

        val p2 = panel()
        p2.addView(label("Dirección de los juegos (web)", 20f, true))
        val input = EditText(this).apply {
            setText(Prefs.gamesUrl(this@SettingsActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        urlInput = input
        p2.addView(input)
        p2.addView(label("El icono del botón Juegos sale de esta web.", 14f, false, soft))

        val p3 = panel()
        p3.addView(label("Seguridad", 20f, true))
        p3.addView(bigButton("🔒  Cambiar PIN", 0xFF6D4C41.toInt()) { changePin() },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val p4 = panel()
        p4.addView(label("Permisos", 20f, true))
        val missing = missingPermissions()
        if (missing.isEmpty()) {
            p4.addView(label("✓ Contactos, teléfono y ubicación concedidos", 16f, false,
                Color.parseColor("#2E7D32")))
        } else {
            val names = missing.map {
                when (it) {
                    Manifest.permission.READ_CONTACTS -> "Contactos"
                    Manifest.permission.CALL_PHONE -> "Teléfono"
                    else -> "Ubicación"
                }
            }
            p4.addView(label("Faltan: ${names.joinToString(", ")}", 16f, false,
                Color.parseColor("#C62828")))
            p4.addView(bigButton("Dar permisos", 0xFF1565C0.toInt()) { askPermissions() },
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        }

        val p5 = panel()
        p5.addView(label("Android", 20f, true))
        p5.addView(bigButton("Abrir ajustes de Android", 0xFF546E7A.toInt()) {
            launch(Settings.ACTION_SETTINGS)
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        p5.addView(bigButton("Cambiar de launcher / salir de Inicio fácil", 0xFF546E7A.toInt()) {
            launch(Settings.ACTION_HOME_SETTINGS)
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
    }

    /** Lista de todas las apps instaladas para añadir una a la pantalla de inicio. */
    @Suppress("DEPRECATION")
    private fun showAppPicker() {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val added = Prefs.customApps(this).map { it.pkg }.toSet()
        val apps: List<ResolveInfo> = pm.queryIntentActivities(launcherIntent, 0)
            .filter {
                val p = it.activityInfo.packageName
                p != packageName && !added.contains(p)
            }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        if (apps.isEmpty()) {
            toast("No hay más apps para añadir")
            return
        }

        val adapter = object : ArrayAdapter<ResolveInfo>(this, 0, apps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val ri = apps[position]
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                }
                row.addView(ImageView(context).apply {
                    setImageDrawable(ri.loadIcon(pm))
                }, LinearLayout.LayoutParams(dp(40), dp(40)))
                row.addView(TextView(context).apply {
                    text = ri.loadLabel(pm)
                    textSize = 18f
                    setPadding(dp(16), 0, 0, 0)
                })
                return row
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Elige una app")
            .setAdapter(adapter) { _, which ->
                val ri = apps[which]
                val list = Prefs.customApps(this)
                list.add(CustomApp(ri.activityInfo.packageName, ri.loadLabel(pm).toString()))
                Prefs.setCustomApps(this, list)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun moveTile(list: List<Tile>, i: Int, delta: Int) {
        val j = i + delta
        if (j < 0 || j >= list.size) return
        val ids = list.map { it.id }.toMutableList()
        Collections.swap(ids, i, j)
        Prefs.setTileOrder(this, ids)
        render()
    }

    private fun moveContact(list: MutableList<Contact>, i: Int, delta: Int) {
        val j = i + delta
        if (j < 0 || j >= list.size) return
        Collections.swap(list, i, j)
        Prefs.setContacts(this, list)
        render()
    }

    @Suppress("DEPRECATION")
    private fun pickContact() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                reqPick
            )
        } catch (e: Exception) {
            toast("No se puede abrir la agenda")
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqPick || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: "Sin nombre"
                    val number = c.getString(2) ?: return
                    val list = Prefs.contacts(this)
                    if (list.none { it.id == id && it.number == number }) {
                        list.add(Contact(id, name, number))
                        Prefs.setContacts(this, list)
                    } else {
                        toast("Ese contacto ya está")
                    }
                }
            }
        } catch (e: Exception) {
            toast("No se pudo leer el contacto")
        }
        render()
    }

    private fun changePin() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Nuevo PIN (mínimo 4 cifras)"
        }
        AlertDialog.Builder(this)
            .setTitle("Cambiar PIN")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val p = input.text.toString()
                if (p.length >= 4) {
                    Prefs.setPin(this, p)
                    toast("PIN cambiado")
                } else {
                    toast("Mínimo 4 cifras")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
