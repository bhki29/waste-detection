package com.example.wastedetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<WasteTypeDetector.BoundingBox> = emptyList()

    // Siapkan "Kuas" Cat
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE // Hanya garis pinggir
        strokeWidth = 8f
        color = Color.GREEN
    }

    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = 180 // Sedikit transparan
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
    }

    // Fungsi untuk menerima data dari Activity
    fun setResults(boundingBoxes: List<WasteTypeDetector.BoundingBox>) {
        this.results = boundingBoxes
        postInvalidate() // Paksa gambar ulang layar
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            // 1. Tentukan Warna Berdasarkan Kelas
            // Index 0 = Organik (Hijau), Index 1 = Anorganik (Merah/Kuning)
            if (result.cls == 0) {
                boxPaint.color = Color.GREEN
            } else {
                boxPaint.color = Color.RED // Atau Color.YELLOW
            }

            // 2. Konversi Koordinat Normal (0.0 - 1.0) ke Piksel Layar
            // left = x1 * lebar_layar, top = y1 * tinggi_layar
            val left = result.x1 * width
            val top = result.y1 * height
            val right = result.x2 * width
            val bottom = result.y2 * height

            val rect = RectF(left, top, right, bottom)

            // 3. Gambar Kotak
            canvas.drawRect(rect, boxPaint)

            // 4. Gambar Teks Label
            val labelText = "${result.label} ${(result.cnf * 100).toInt()}%"
            val textWidth = textPaint.measureText(labelText)
            val textHeight = textPaint.textSize

            // Gambar latar belakang teks (biar terbaca)
            canvas.drawRect(
                left,
                top - textHeight - 10f,
                left + textWidth + 20f,
                top,
                textBackgroundPaint
            )

            // Tulis teksnya
            canvas.drawText(labelText, left + 10f, top - 10f, textPaint)
        }
    }
}