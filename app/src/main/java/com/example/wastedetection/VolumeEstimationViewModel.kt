package com.example.wastedetection

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VolumeEstimationViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Inisialisasi Helper Class yang kita buat di Step 2
    private val estimator = WasteVolumeEstimator(application.applicationContext)

    // 2. LiveData: "Tempat penampungan" hasil yang bisa dipantau oleh UI
    // _predictionResult bersifat private (hanya bisa diubah di sini)
    // predictionResult bersifat public (hanya bisa dibaca oleh UI)
    private val _predictionResult = MutableLiveData<String>()
    val predictionResult: LiveData<String> = _predictionResult

    private val _confidenceScore = MutableLiveData<String>()
    val confidenceScore: LiveData<String> = _confidenceScore

    // 3. Fungsi Utama: Menerima Bitmap, lalu proses di Background Thread
    fun detectVolume(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            // Panggil fungsi predict() dari Helper Class
            val result = estimator.predict(bitmap)

            // Update LiveData (harus menggunakan postValue karena dari background thread)
            _predictionResult.postValue(result.volumeLabel)

            // Format confidence score ke persen (misal: 0.95 -> 95%)
            val confidencePercent = (result.confidence * 100).toInt()
            _confidenceScore.postValue("Akurasi: $confidencePercent%")
        }
    }
}