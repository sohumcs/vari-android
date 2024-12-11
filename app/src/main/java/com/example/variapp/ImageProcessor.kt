package com.example.variapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

fun splitImageIntoParts(image: Bitmap): List<Bitmap> {
    val parts = mutableListOf<Bitmap>()
    val partWidth = image.width / 4
    val partHeight = image.height / 4
    for (i in 0 until 4) {
        for (j in 0 until 4) {
            val part = Bitmap.createBitmap(image, j * partWidth, i * partHeight, partWidth, partHeight)
            parts.add(part)
        }
    }
    return parts
}

fun calculateVARI(part: Bitmap): Double {
    var sumVARI = 0.0
    val pixels = IntArray(part.width * part.height)
    part.getPixels(pixels, 0, part.width, 0, 0, part.width, part.height)

    for (pixel in pixels) {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        val vari = if ((green + red - blue) != 0) {
            (green - red).toDouble() / (green + red - blue).toDouble()
        } else 0.0

        sumVARI += vari
    }

    return sumVARI / pixels.size
}

fun generateHeatmap(variIndices: List<Double>, gridSize: Int): Bitmap {
    val heatmap = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(heatmap)
    val paint = Paint()

    for ((index, vari) in variIndices.withIndex()) {
        paint.color = when {
            vari < 0.2 -> Color.RED
            vari < 0.5 -> Color.YELLOW
            else -> Color.GREEN
        }
        val x = (index % 4) * (heatmap.width / 4)
        val y = (index / 4) * (heatmap.height / 4)
        canvas.drawRect(Rect(x, y, x + heatmap.width / 4, y + heatmap.height / 4), paint)
    }

    return heatmap
}
