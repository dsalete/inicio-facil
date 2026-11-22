package com.familia.inicio

import android.content.Context
import android.widget.ImageView

/** Imagen siempre cuadrada: usa el lado más pequeño del espacio disponible. */
class SquareImageView(context: Context) : ImageView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }
}
