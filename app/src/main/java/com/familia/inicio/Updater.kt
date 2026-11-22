package com.familia.inicio

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Una versión publicada: su número (el de la compilación) y la dirección del APK. */
class UpdateInfo(val version: Int, val url: String)

/**
 * Busca versiones nuevas en las Releases de GitHub, las descarga y lanza la instalación.
 * Android no deja instalar en silencio: el sistema pedirá pulsar «Instalar/Actualizar».
 */
object Updater {

    /** Repositorio PÚBLICO de GitHub donde el compilador publica los APK (Releases). */
    const val REPO = "dsalete/inicio-facil"

    private const val CHECK_EVERY_MS = 12L * 60 * 60 * 1000

    @Suppress("DEPRECATION")
    fun installedVersion(ctx: Context): Int {
        return try {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
        } catch (e: Exception) {
            0
        }
    }

    /** Última versión publicada, o null si no hay ninguna. Lanza excepción si no hay conexión. */
    private fun fetchLatest(): UpdateInfo? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "InicioFacil")
        try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val number = json.getString("tag_name").removePrefix("v").toIntOrNull() ?: return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    return UpdateInfo(number, a.getString("browser_download_url"))
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun download(ctx: Context, url: String): File? {
        val file = File(ctx.cacheDir, "update.apk")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "InicioFacil")
        try {
            if (conn.responseCode != 200) return null
            conn.inputStream.use { input ->
                file.outputStream().use { out -> input.copyTo(out) }
            }
            return file
        } finally {
            conn.disconnect()
        }
    }

    private fun toast(ctx: Context, s: String) =
        Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    /** Avisa solo si han pasado 12 horas desde la última comprobación y está activado. */
    fun checkIfDue(act: Activity) {
        if (!Prefs.updateAuto(act)) return
        if (System.currentTimeMillis() - Prefs.lastUpdateCheck(act) < CHECK_EVERY_MS) return
        check(act, false)
    }

    /** Comprueba si hay versión nueva. Con [manual] también avisa cuando no la hay o falla. */
    fun check(act: Activity, manual: Boolean) {
        Thread {
            var found: UpdateInfo? = null
            var failed = false
            try {
                found = fetchLatest()
            } catch (e: Exception) {
                failed = true
            }
            val info = found
            act.runOnUiThread {
                if (act.isFinishing || act.isDestroyed) return@runOnUiThread
                if (!failed) Prefs.setLastUpdateCheck(act, System.currentTimeMillis())
                if (failed) {
                    if (manual) toast(act, "No se pudo comprobar. ¿Hay internet?")
                } else if (info == null) {
                    if (manual) toast(act, "No hay ninguna versión publicada todavía")
                } else if (info.version > installedVersion(act)) {
                    offer(act, info)
                } else if (manual) {
                    toast(act, "Ya tienes la última versión")
                }
            }
        }.start()
    }

    private fun offer(act: Activity, info: UpdateInfo) {
        val dialog = AlertDialog.Builder(act)
            .setTitle("Versión nueva")
            .setMessage("Hay una versión nueva de Inicio fácil. ¿Instalar ahora?")
            .setPositiveButton("Instalar") { _, _ -> downloadAndInstall(act, info) }
            .setNegativeButton("Más tarde", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 22f
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 20f
    }

    private fun downloadAndInstall(act: Activity, info: UpdateInfo) {
        val dialog = AlertDialog.Builder(act)
            .setTitle("Actualizando")
            .setMessage("Descargando la versión nueva…")
            .setCancelable(false)
            .create()
        dialog.show()
        Thread {
            val file = try {
                download(act, info.url)
            } catch (e: Exception) {
                null
            }
            act.runOnUiThread {
                if (act.isFinishing || act.isDestroyed) return@runOnUiThread
                dialog.dismiss()
                if (file == null) {
                    toast(act, "No se pudo descargar la actualización")
                } else {
                    install(act, file)
                }
            }
        }.start()
    }

    private fun install(act: Activity, file: File) {
        try {
            val installer = act.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(act.packageName)
            if (Build.VERSION.SDK_INT >= 31) {
                // Si Android lo permite, actualiza sin pedir confirmación.
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val id = installer.createSession(params)
            val session = installer.openSession(id)
            try {
                session.openWrite("inicio-facil.apk", 0, file.length()).use { out ->
                    file.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(act, InstallReceiver::class.java).setPackage(act.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pi = PendingIntent.getBroadcast(act, id, intent, flags)
                session.commit(pi.intentSender)
            } catch (e: Exception) {
                session.abandon()
                throw e
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            toast(act, "No se pudo instalar la actualización")
        }
    }
}
