package com.example.variapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var heatmapView: ImageView
    private lateinit var resultTextView: TextView
    private val IMAGE_PICK_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Link the layout file to this activity
        setContentView(R.layout.activity_main)

        // Initialize UI components
        imageView = findViewById(R.id.imageView)
        heatmapView = findViewById(R.id.heatmapView)
        resultTextView = findViewById(R.id.resultTextView) // Ensure this matches the XML ID
        val uploadButton: Button = findViewById(R.id.uploadButton)

        // Set a listener for the upload button
        uploadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            val selectedImageUri = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)

            // Display the selected image
            imageView.setImageBitmap(bitmap)

            // Process the image, generate a heatmap, and display it
            val parts = splitImageIntoParts(bitmap)
            val variIndices = parts.map { calculateVARI(it) }
            val heatmap = generateHeatmap(variIndices, bitmap.width, bitmap.height)

            heatmapView.setImageBitmap(heatmap)

            // Display the area metrics
            val areaMetrics = generateAreaMetrics(variIndices)
            resultTextView.text = areaMetrics // Display the area metrics text
        }
    }

    // Function to split the image into 16 parts (4x4 grid)
    private fun splitImageIntoParts(bitmap: Bitmap): List<Bitmap> {
        val parts = mutableListOf<Bitmap>()
        val width = bitmap.width
        val height = bitmap.height
        val partWidth = width / 4
        val partHeight = height / 4

        for (i in 0..3) {
            for (j in 0..3) {
                val x = j * partWidth
                val y = i * partHeight
                val part = Bitmap.createBitmap(bitmap, x, y, partWidth, partHeight)
                parts.add(part)
            }
        }
        return parts
    }

    // Function to calculate the VARI (Vegetation Adjusted Index) for an image part
    private fun calculateVARI(part: Bitmap): Float {
        val width = part.width
        val height = part.height
        var sumRed = 0f
        var sumGreen = 0f
        var sumBlue = 0f

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = part.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                sumRed += red
                sumGreen += green
                sumBlue += blue
            }
        }

        // Calculate VARI
        return (sumGreen - sumRed) / (sumGreen + sumRed - sumBlue)
    }

    // Function to generate the heatmap based on VARI indices
    private fun generateHeatmap(variIndices: List<Float>, width: Int, height: Int): Bitmap {
        val heatmapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val vari = variIndices[i * 4 + j]
                val color = when {
                    vari <= -0.5 -> Color.RED // Very Barren
                    vari <= -0.2 -> Color.parseColor("#FFA500") // Barren (Orange)
                    vari <= 0.2 -> Color.YELLOW // Plain
                    vari <= 0.5 -> Color.parseColor("#90EE90") // Healthy (Light Green)
                    else -> Color.GREEN // Very Healthy
                }
                val x = j * (width / 4)
                val y = i * (height / 4)
                for (dx in 0 until (width / 4)) {
                    for (dy in 0 until (height / 4)) {
                        heatmapBitmap.setPixel(x + dx, y + dy, color)
                    }
                }
            }
        }

        return heatmapBitmap
    }

    // Function to generate area metrics based on VARI values
    private fun generateAreaMetrics(variIndices: List<Float>): String {
        val areaMetrics = StringBuilder()
        val areas = listOf("Top Left", "Top Right", "Bottom Left", "Bottom Right")

        for (i in 0 until 16) {
            val vari = variIndices[i]
            val area = areas[i % 4] + " (${(i / 4) + 1})"
            val classification = when {
                vari <= -0.5 -> "Very Barren"
                vari <= -0.2 -> "Barren"
                vari <= 0.2 -> "Plain"
                vari <= 0.5 -> "Healthy"
                else -> "Very Healthy"
            }
            areaMetrics.append("$area: $classification (VARI: %.2f)\n".format(vari))
        }

        return areaMetrics.toString()
    }
}
