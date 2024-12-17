package com.example.variapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

// Function to split the image into parts
fun splitImageIntoParts(image: Bitmap, gridSize: Int): List<Bitmap> {
    val parts = mutableListOf<Bitmap>()
    val partWidth = image.width / gridSize
    val partHeight = image.height / gridSize

    for (i in 0 until gridSize) {
        for (j in 0 until gridSize) {
            val part = Bitmap.createBitmap(image, j * partWidth, i * partHeight, partWidth, partHeight)
            parts.add(part)
        }
    }
    return parts
}

// Function to calculate VARI for a single image part
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

// Temporal averaging function
fun temporalAveraging(variLists: List<List<Double>>): List<Double> {
    val numParts = variLists[0].size
    val averagedVari = MutableList(numParts) { 0.0 }

    for (partIndex in 0 until numParts) {
        var sum = 0.0
        for (variList in variLists) {
            sum += variList[partIndex]
        }
        averagedVari[partIndex] = sum / variLists.size
    }
    return averagedVari
}

// Function to generate heatmap from averaged VARI values
fun generateHeatmap(variIndices: List<Double>, imageWidth: Int, imageHeight: Int, gridSize: Int): Bitmap {
    val heatmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(heatmap)
    val paint = Paint()

    val partWidth = imageWidth / gridSize
    val partHeight = imageHeight / gridSize

    for (i in 0 until gridSize) {
        for (j in 0 until gridSize) {
            val vari = variIndices[i * gridSize + j]
            val color = when {
                vari <= -0.5 -> Color.RED // Very Barren
                vari <= -0.2 -> Color.parseColor("#FFA500") // Barren (Orange)
                vari <= 0.2 -> Color.YELLOW // Plain
                vari <= 0.5 -> Color.parseColor("#90EE90") // Healthy (Light Green)
                else -> Color.GREEN // Very Healthy
            }

            paint.color = color
            canvas.drawRect(
                (j * partWidth).toFloat(),
                (i * partHeight).toFloat(),
                ((j + 1) * partWidth).toFloat(),
                ((i + 1) * partHeight).toFloat(),
                paint
            )
        }
    }

    return heatmap
}
