package com.example.wastedetection.WasteDetection

import android.graphics.RectF

// Data class otomatis membuatkan getter, setter, toString, dan equals
data class DetectionResult(
    val label: String,      // "Organik" atau "Anorganik"
    val score: Float,       // Tingkat keyakinan (0.0 - 1.0)
    val boundingBox: RectF  // Koordinat kotak (left, top, right, bottom)
)
