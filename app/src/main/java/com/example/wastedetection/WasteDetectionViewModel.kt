package com.example.wastedetection

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WasteDetectionViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Panggil Helper Class (Otak YOLO) yang kita buat di Step 2
    private val detector = WasteTypeDetector(application.applicationContext)

    // 2. LiveData untuk menampung hasil deteksi
    // Isinya adalah List of BoundingBox (karena bisa jadi ada banyak sampah dalam 1 layar)
    private val _detectionResult = MutableLiveData<List<WasteTypeDetector.BoundingBox>>()
    val detectionResult: LiveData<List<WasteTypeDetector.BoundingBox>> = _detectionResult

    // LiveData untuk waktu proses (Opsional, untuk debug kecepatan)
    private val _inferenceTime = MutableLiveData<Long>()
    val inferenceTime: LiveData<Long> = _inferenceTime

    /**
     * Fungsi utama yang dipanggil oleh Kamera setiap kali ada frame baru
     */
    fun detect(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            // A. Lakukan Deteksi (Berat! Makanya di Dispatchers.IO)
            val results = detector.detect(bitmap)

            val endTime = System.currentTimeMillis()

            // B. Kirim hasil ke UI
            _detectionResult.postValue(results)
            _inferenceTime.postValue(endTime - startTime)
        }
    }
}