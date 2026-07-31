package com.example.wastedetection.WasteVolumeEstimation

import java.util.Locale

class VolumeCalculator {

    // Fungsi untuk membersihkan teks AI (Contoh: "100 %" menjadi 100.0)
    fun parsePercentage(rawText: String): Double {
        val cleanString = rawText.replace("%", "").trim()
        return cleanString.toDoubleOrNull() ?: 0.0
    }

    // Fungsi rumus utama sesuai instruksi dosen (Persentase / 100) * Liter
    fun calculateVolume(percentage: Double, containerVolumeLiter: Double): String {
        val volumeInLiter = (percentage / 100.0) * containerVolumeLiter

        // Mengembalikan string dengan format 2 angka di belakang koma
        return String.format(Locale.US, "%.2f", volumeInLiter)
    }
}