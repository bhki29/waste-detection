package com.example.wastedetection.WasteDetection

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val detector by lazy { YoloDetector(application) }

    // --- LiveData Baru (Dipisah agar sesuai desain Figma) ---
    private val _resultBitmap = MutableLiveData<Bitmap>()
    val resultBitmap: LiveData<Bitmap> = _resultBitmap

    private val _organikCount = MutableLiveData<Int>()
    val organikCount: LiveData<Int> = _organikCount

    private val _anorganikCount = MutableLiveData<Int>()
    val anorganikCount: LiveData<Int> = _anorganikCount

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> = _totalCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val TAG = "SKRIPSI_DEBUG"

    fun processImage(originalBitmap: Bitmap) {
        _isLoading.value = true
        Log.d(TAG, "🔵 [ViewModel] Perintah diterima. Bersiap memproses gambar...")

        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (originalBitmap != null) {
                    Log.d(TAG, "🧐 STATUS: GAMBAR SEDANG DIKIRIM KE MODEL YOLO...")
                    Log.d(TAG, "   👉 Ukuran Gambar: ${originalBitmap.width} x ${originalBitmap.height} px")
                }

                val rawResults = detector.detect(originalBitmap)

                val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)

                val boxPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                }
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    style = Paint.Style.FILL
                }
                val textBgPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                    alpha = 160
                }

                var countOrganik = 0
                var countAnorganik = 0

                for (result in rawResults) {
                    var box = result.boundingBox

                    if (box.width() < 10 && box.height() < 10) {
                        val newLeft = box.left * originalBitmap.width
                        val newTop = box.top * originalBitmap.height
                        val newRight = box.right * originalBitmap.width
                        val newBottom = box.bottom * originalBitmap.height

                        box = RectF(newLeft, newTop, newRight, newBottom)
                    }

                    if (box.right < (originalBitmap.width / 10)) {
                        if (box.right < 100) {
                            val newLeft = box.left * originalBitmap.width
                            val newTop = box.top * originalBitmap.height
                            val newRight = box.right * originalBitmap.width
                            val newBottom = box.bottom * originalBitmap.height
                            box = RectF(newLeft, newTop, newRight, newBottom)
                        }
                    }

                    if (result.label.contains("Organik", ignoreCase = true) && !result.label.contains("An")) {
                        boxPaint.color = Color.GREEN
                        countOrganik++
                    } else {
                        boxPaint.color = Color.RED
                        countAnorganik++
                    }

                    canvas.drawRect(box, boxPaint)

                    val labelText = "${result.label} ${(result.score * 100).toInt()}%"
                    val textWidth = textPaint.measureText(labelText)
                    val textBgRect = RectF(box.left, box.top - 50f, box.left + textWidth + 20f, box.top)
                    canvas.drawRect(textBgRect, textBgPaint)
                    canvas.drawText(labelText, box.left + 10f, box.top - 10f, textPaint)
                }

                // 🔧 PERUBAHAN DISINI: Mengirim 3 angka secara terpisah
                withContext(Dispatchers.Main) {
                    _resultBitmap.value = mutableBitmap
                    _organikCount.value = countOrganik
                    _anorganikCount.value = countAnorganik
                    _totalCount.value = countOrganik + countAnorganik
                    _isLoading.value = false
                    Log.d(TAG, "✅ [ViewModel] SUKSES! Data dikirim ke UI.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "🔴 ERROR: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}