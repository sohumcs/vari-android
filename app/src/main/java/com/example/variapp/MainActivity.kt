package com.example.variapp

import android.content.Intent
import android.graphics.Bitmap
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
            val parts = splitImageIntoParts(bitmap, 16) // Updated grid size to 16x16
            val variIndices = parts.map { calculateVARI(it) }
            val heatmap = generateHeatmap(variIndices, bitmap.width, bitmap.height, 16) // Updated grid size to 16x16

            heatmapView.setImageBitmap(heatmap)

            // Display the area metrics
            val areaMetrics = generateAreaMetrics(variIndices, 16) // Updated grid size to 16x16
            resultTextView.text = areaMetrics
        }
    }

    // Function to split the image into 256 parts (16x16 grid)
    private fun splitImageIntoParts(bitmap: Bitmap, gridSize: Int): List<Bitmap> {
        val parts = mutableListOf<Bitmap>()
        val width = bitmap.width
        val height = bitmap.height
        val partWidth = width / gridSize
        val partHeight = height / gridSize

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val x = j * partWidth
                val y = i * partHeight
                val part = Bitmap.createBitmap(bitmap, x, y, partWidth, partHeight)
                parts.add(part)
            }
        }
        return parts
    }

    // Function to calculate the VARI (Vegetation Adjusted Index) for an image part
    private fun calculateVARI(part: Bitmap): Double {
        val width = part.width
        val height = part.height
        var sumRed = 0.0
        var sumGreen = 0.0
        var sumBlue = 0.0

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
        return if ((sumGreen + sumRed - sumBlue) != 0.0) {
            (sumGreen - sumRed) / (sumGreen + sumRed - sumBlue)
        } else {
            0.0
        }
    }

    // Function to generate the heatmap based on VARI indices
    private fun generateHeatmap(variIndices: List<Double>, width: Int, height: Int, gridSize: Int): Bitmap {
        val heatmapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val partWidth = width / gridSize
        val partHeight = height / gridSize

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val vari = variIndices[i * gridSize + j]
                val color = when {
                    vari <= -0.5 -> android.graphics.Color.RED // Very Barren
                    vari <= -0.2 -> android.graphics.Color.parseColor("#FFA500") // Barren (Orange)
                    vari <= 0.2 -> android.graphics.Color.YELLOW // Plain
                    vari <= 0.5 -> android.graphics.Color.parseColor("#90EE90") // Healthy (Light Green)
                    else -> android.graphics.Color.GREEN // Very Healthy
                }
                val x = j * partWidth
                val y = i * partHeight
                for (dx in 0 until partWidth) {
                    for (dy in 0 until partHeight) {
                        heatmapBitmap.setPixel(x + dx, y + dy, color)
                    }
                }
            }
        }

        return heatmapBitmap
    }

    // Function to generate area metrics based on VARI values
    private fun generateAreaMetrics(variIndices: List<Double>, gridSize: Int): String {
        val areaMetrics = StringBuilder()

        for (i in variIndices.indices) {
            val vari = variIndices[i]
            val row = i / gridSize + 1
            val col = i % gridSize + 1
            val classification = when {
                vari <= -0.5 -> "Very Barren"
                vari <= -0.2 -> "Barren"
                vari <= 0.2 -> "Plain"
                vari <= 0.5 -> "Healthy"
                else -> "Very Healthy"
            }
            areaMetrics.append("Part ($row, $col): $classification (VARI: %.2f)\n".format(vari))
        }

        return areaMetrics.toString()
    }
}
