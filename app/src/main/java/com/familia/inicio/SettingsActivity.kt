package com.familia.inicio

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Collections

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Ajustes solo para ti (se entra con PIN desde la pantalla de inicio). */
class SettingsActivity : Activity() {

    private val reqPick = 42
    private val reqPerm = 43

    private lateinit var scroll: ScrollView
    private lateinit var box: LinearLayout
    private var urlInput: EditText? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun heading(s: String, size: Float = 20f) = TextView(this).apply {
        text = s
        textSize = size
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun smallButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        minWidth = 0
        minimumWidth = dp(52)
        setOnClickListener { onClick() }
    }

    /** Botón que, al pulsarlo, muestra una lista de opciones para elegir una. */
    private fun choice(
        titleText: String,
        options: List<String>,
        selected: Int,
        onPick: (Int) -> Unit
    ): Button {
        val b = Button(this)
        b.text = options[selected]
        b.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(titleText)
                .setSingleChoiceItems(
                    options.toTypedArray(),
                    options.indexOf(b.text.toString())
                ) { d, which ->
                    onPick(which)
                    b.text = options[which]
                    d.dismiss()
                }
                .show()
        }
        return b
    }

    private fun launch(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            toast("No se puede abrir")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = dp(16)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        scroll = ScrollView(this).apply {
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

    private fun saveUrl() {
        val url = urlInput?.text?.toString()?.trim() ?: return
        if (url.startsWith("http://") || url.startsWith("https://")) {
            Prefs.setGamesUrl(this, url)
        }
    }

    // Permisos: leer contactos (para las fotos) y llamar directamente.
    private fun askPermissions() {
        val need = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), reqPerm)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }

    /** Dibuja toda la pantalla de ajustes (se repite al cambiar el orden o los contactos). */
    private fun render() {
        saveUrl()
        val y = scroll.scrollY
        box.removeAllViews()

        box.addView(heading("Ajustes (solo para ti)", 26f))

        // --- Botones de la pantalla de inicio: visibilidad y orden ---
        box.addView(heading("Botones de inicio (visibilidad y orden)"))
        val tiles = Tiles.ordered(this)
        tiles.forEachIndexed { i, t ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(CheckBox(this).apply {
                text = if (t.pkg != null) t.label else "${t.emoji}  ${t.label}"
                textSize = 18f
                isChecked = Prefs.visible(this@SettingsActivity, t.id)
                setOnCheckedChangeListener { _, v ->
                    Prefs.setVisible(this@SettingsActivity, t.id, v)
                }
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            row.addView(smallButton("▲") { moveTile(tiles, i, -1) })
            row.addView(smallButton("▼") { moveTile(tiles, i, 1) })
            if (t.pkg != null) {
                // Las apps añadidas se pueden quitar del todo.
                row.addView(smallButton("✕") {
                    val list = Prefs.customApps(this)
                    list.removeAll { it.pkg == t.pkg }
                    Prefs.setCustomApps(this, list)
                    render()
                })
            }
            box.addView(row)
        }
        box.addView(Button(this).apply {
            text = "Añadir app"
            setOnClickListener { showAppPicker() }
        })

        // --- Contactos para la pantalla Llamar ---
        box.addView(heading("Contactos para llamar (6 por página)"))
        val contacts = Prefs.contacts(this)
        if (contacts.isEmpty()) {
            box.addView(TextView(this).apply {
                text = "Aún no hay contactos. Pulsa «Añadir contacto»."
                textSize = 16f
            })
        }
        contacts.forEachIndexed { i, k ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(this).apply {
                text = k.name
                textSize = 18f
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            row.addView(smallButton("▲") { moveContact(contacts, i, -1) })
            row.addView(smallButton("▼") { moveContact(contacts, i, 1) })
            row.addView(smallButton("✕") {
                contacts.removeAt(i)
                Prefs.setContacts(this, contacts)
                render()
            })
            box.addView(row)
        }
        box.addView(Button(this).apply {
            text = "Añadir contacto"
            setOnClickListener { pickContact() }
        })

        // --- Tamaño del texto ---
        box.addView(heading("Tamaño del texto"))
        box.addView(
            choice(
                "Tamaño del texto",
                listOf("Pequeño", "Normal", "Grande"),
                Prefs.textSize(this).coerceIn(0, 2)
            ) { Prefs.setTextSize(this, it) }
        )

        // --- Toques repetidos ---
        box.addView(heading("Ignorar toques repetidos"))
        val times = longArrayOf(0L, 500L, 1000L, 2000L)
        box.addView(
            choice(
                "Ignorar toques repetidos",
                listOf("No ignorar", "Medio segundo", "1 segundo", "2 segundos"),
                times.indexOf(Prefs.debounceMs(this)).coerceAtLeast(0)
            ) { Prefs.setDebounceMs(this, times[it]) }
        )

        // --- Dirección de la web de juegos ---
        box.addView(heading("Dirección de los juegos (web)"))
        val input = EditText(this).apply {
            setText(Prefs.gamesUrl(this@SettingsActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        urlInput = input
        box.addView(input)

        // --- PIN ---
        box.addView(heading("PIN de ajustes"))
        box.addView(Button(this).apply {
            text = "Cambiar PIN"
            setOnClickListener { changePin() }
        })

        // --- Android ---
        box.addView(heading("Android"))
        box.addView(Button(this).apply {
            text = "Abrir ajustes de Android"
            setOnClickListener { launch(Settings.ACTION_SETTINGS) }
        })
        box.addView(Button(this).apply {
            text = "Cambiar de launcher / salir de Inicio fácil"
            setOnClickListener { launch(Settings.ACTION_HOME_SETTINGS) }
        })

        box.addView(Button(this).apply {
            text = "Volver"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(24) })

        scroll.post { scroll.scrollTo(0, y) }
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
        val phone = ContactsContract.CommonDataKinds.Phone
        try {
            contentResolver.query(
                uri,
                arrayOf(phone.CONTACT_ID, phone.DISPLAY_NAME, phone.NUMBER),
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
