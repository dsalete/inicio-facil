package com.familia.inicio

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.ContactsContract
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Utilidades compartidas para dibujar contactos y botones redondeados. */
object ContactUi {

    private val colors = intArrayOf(
        0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(), 0xFFEF6C00.toInt(),
        0xFFAB47BC.toInt(), 0xFF8D6E63.toInt(), 0xFF42A5F5.toInt()
    )

    fun rounded(color: Int, radiusPx: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx.toFloat()
    }

    /** Color de fondo fijo de un contacto (cuando no tiene foto). */
    fun colorFor(c: Contact): Int = colors[(c.id % colors.size).toInt()]

    /** Foto del contacto en la agenda, o null si no tiene (o no hay permiso). */
    fun photo(ctx: Context, contactId: Long): Bitmap? {
        return try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val stream = ContactsContract.Contacts.openContactPhotoInputStream(
                ctx.contentResolver, uri, true
            )
            stream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    /** Nombre más legible: si viene todo en mayúsculas («ANA PÉREZ»), pasa a «Ana Pérez». */
    fun pretty(name: String): String {
        val n = name.trim()
        if (n != n.uppercase()) return n
        return n.lowercase().split(" ").filter { it.isNotEmpty() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    /** Foto del contacto a pantalla completa (recortada), o un fondo de color con su inicial. */
    fun fill(ctx: Context, c: Contact): View {
        val bmp = photo(ctx, c.id)
        if (bmp != null) {
            val iv = ImageView(ctx)
            iv.setImageBitmap(bmp)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            return iv
        }
        val frame = FrameLayout(ctx)
        frame.setBackgroundColor(colors[(c.id % colors.size).toInt()])
        val tv = TextView(ctx)
        tv.text = c.name.trim().firstOrNull()?.uppercase() ?: "?"
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.setAutoSizeTextTypeUniformWithConfiguration(24, 110, 2, TypedValue.COMPLEX_UNIT_SP)
        frame.addView(
            tv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return frame
    }

    /** Círculo con la foto del contacto o, si no tiene, su inicial sobre un color. */
    fun avatar(ctx: Context, c: Contact, sizePx: Int): View {
        val frame = FrameLayout(ctx)
        frame.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        frame.clipToOutline = true

        val bmp = photo(ctx, c.id)
        if (bmp != null) {
            val iv = ImageView(ctx)
            iv.setImageBitmap(bmp)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            frame.addView(iv, FrameLayout.LayoutParams(sizePx, sizePx))
        } else {
            val tv = TextView(ctx)
            tv.text = c.name.trim().firstOrNull()?.uppercase() ?: "?"
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.45f)
            tv.setTypeface(tv.typeface, Typeface.BOLD)
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER
            tv.setBackgroundColor(colors[(c.id % colors.size).toInt()])
            frame.addView(tv, FrameLayout.LayoutParams(sizePx, sizePx))
        }
        return frame
    }
}
