package com.example.wastedetection

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

/**
 * Class ini bertugas sebagai jembatan antara Aplikasi Android dan Model TFLite.
 * Semua logika rumit AI disembunyikan di sini.
 */
class WasteVolumeEstimator(context: Context) {

    // Konfigurasi Model sesuai Skripsi & Diagram
    companion object {
        private const val MODEL_NAME = "model_prediksi_volume_sampah.tflite"
        private const val INPUT_SIZE = 128 // Sesuai diagram node 'keras_tensor'
    }

    private var interpreter: Interpreter? = null

    // Label Output (0% - 100% kenaikan 10%)
    private val labels = listOf(
        "0 %", "10 %", "20 %", "30 %", "40 %", "50 %",
        "60 %", "70 %", "80 %", "90 %", "100 %"
    )

    init {
        // Inisialisasi Model saat class dibuat
        setupInterpreter(context)
    }

    private fun setupInterpreter(context: Context) {
        val options = Interpreter.Options()
        options.setNumThreads(4) // Menggunakan 4 thread CPU agar lebih cepat

        try {
            // Memuat file model dari folder assets
            val modelFile: ByteBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun predict(bitmap: Bitmap): PredictionResult {
        if (interpreter == null) {
            return PredictionResult("Error", 0f)
        }

        // 1. Pre-processing: Siapkan Gambar
        // - Resize ke 128x128
        // - Normalize (Nilai piksel dibagi 255.0 agar jadi 0.0 - 1.0)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // Rumus: (pixel - 0) / 255
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Siapkan wadah untuk Output
        // Model mengeluarkan array 2D [1][11] (1 gambar, 11 probabilitas kelas)
        val outputBuffer = Array(1) { FloatArray(11) }

        // 3. Jalankan Inference (Prediksi)
        interpreter?.run(tensorImage.buffer, outputBuffer)

        // 4. Post-processing: Cari nilai tertinggi
        val result = getMaxResult(outputBuffer[0])

        return result
    }

    private fun getMaxResult(probabilities: FloatArray): PredictionResult {
        var maxIndex = 0
        var maxConfidence = 0f

        // Loop untuk mencari index dengan probabilitas tertinggi
        for (i in probabilities.indices) {
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i]
                maxIndex = i
            }
        }

        // Ambil label berdasarkan index pemenang
        val label = if (maxIndex in labels.indices) labels[maxIndex] else "Unknown"

        return PredictionResult(label, maxConfidence)
    }

    // Class kecil untuk membungkus hasil agar rapi
    data class PredictionResult(
        val volumeLabel: String,   // Contoh: "70 %"
        val confidence: Float      // Contoh: 0.95 (95%)
    )
}