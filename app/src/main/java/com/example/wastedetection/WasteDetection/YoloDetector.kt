package com.example.wastedetection.WasteDetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import android.util.Log // Tambahkan ini

class YoloDetector(context: Context) {

    // Konfigurasi Model
    private val MODEL_FILE = "best_float32.tflite"
    private val LABELS = listOf("Sampah Anorganik", "Sampah Organik") // Sesuaikan urutan training!
    private val INPUT_SIZE = 640
    private val CONFIDENCE_THRESHOLD = 0.50f
    private val IOU_THRESHOLD = 0.45f
    private val TAG = "SKRIPSI_DEBUG" // Nama CCTV kita

    // Inisialisasi Interpreter TFLite
    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options()
        // options.setUseNNAPI(true) // Uncomment jika ingin pakai Hardware Acceleration
        Interpreter(loadModelFile(context), options)
    }

    // 1. Fungsi Membaca File Model dari Assets
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    // 2. Fungsi Utama: Deteksi Gambar
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        // CCTV 1: Cek Input
        Log.d(TAG, "🟢 [1] Memulai Deteksi... Ukuran Gambar: ${bitmap.width}x${bitmap.height}")
        val startTime = System.currentTimeMillis() // Catat waktu mulai

        // A. Pre-processing Gambar (Resize ke 640x640 & Normalisasi 0-255 ke 0-1)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // B. Siapkan Wadah Output
        // Output YOLOv8: [1, 6, 8400] -> (Batch, Attributes, Anchors)
        // Attributes: [x, y, w, h, score_class1, score_class2]
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 6, 8400), DataType.FLOAT32)

        // C. Jalankan Model (Inference)
        try {
            interpreter.run(tensorImage.buffer, outputBuffer.buffer)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 ERROR saat interpreter.run: ${e.message}")
            return emptyList()
        }

        // D. Parsing Output (Menerjemahkan Angka jadi Kotak)
        val outputArray = outputBuffer.floatArray
        val results = ArrayList<DetectionResult>()

//        val rows = 6 // x, y, w, h, score1, score2
        val columns = 8400 // Jumlah kotak prediksi

        // Loop menelusuri 8400 prediksi
        for (i in 0 until columns) {
            // Ambil score anorganik (index 4) dan organik (index 5)
            // Rumus akses array flat: [row * columns + col]
            val scoreAnorganik = outputArray[4 * columns + i]
            val scoreOrganik = outputArray[5 * columns + i]

            // Cari score tertinggi
            val maxScore = if (scoreAnorganik > scoreOrganik) scoreAnorganik else scoreOrganik
            val labelIndex = if (scoreAnorganik > scoreOrganik) 0 else 1

            // Filter awal: Hanya ambil yang yakin > 50%
            if (maxScore > CONFIDENCE_THRESHOLD) {
                // Ambil koordinat (x, y, w, h)
                val cx = outputArray[0 * columns + i]
                val cy = outputArray[1 * columns + i]
                val w = outputArray[2 * columns + i]
                val h = outputArray[3 * columns + i]

                // Konversi koordinat dari (Center X, Center Y) ke (Left, Top, Right, Bottom)
                // Dan kembalikan skala ke ukuran gambar asli
                val scaleX = bitmap.width.toFloat() / INPUT_SIZE
                val scaleY = bitmap.height.toFloat() / INPUT_SIZE

                val left = (cx - w / 2) * scaleX
                val top = (cy - h / 2) * scaleY
                val right = (cx + w / 2) * scaleX
                val bottom = (cy + h / 2) * scaleY

                results.add(
                    DetectionResult(
                        LABELS[labelIndex],
                        maxScore,
                        RectF(left, top, right, bottom)
                    )
                )
            }
        }

        Log.d(TAG, "🟡 [2] Kandidat Awal (Sebelum NMS): ${results.size} kotak")

        // E. Lakukan NMS (Non-Maximum Suppression) untuk buang kotak duplikat
        val finalResults = applyNMS(results)

        val endTime = System.currentTimeMillis() // Catat waktu selesai
        val duration = endTime - startTime

        Log.d(TAG, "🟢 [3] Hasil Akhir (Setelah NMS): ${finalResults.size} kotak")
        Log.d(TAG, "⏱️ Waktu Proses: $duration ms") // Data penting untuk Bab 4 Skripsi!

        return finalResults
    }

    // Algoritma NMS (Membersihkan tumpukan kotak)
    private fun applyNMS(boxes: MutableList<DetectionResult>): List<DetectionResult> {
        val finalBoxes = ArrayList<DetectionResult>()

        // Urutkan berdasarkan score tertinggi
        boxes.sortByDescending { it.score }

        while (boxes.isNotEmpty()) {
            val bestBox = boxes.removeAt(0)
            finalBoxes.add(bestBox)

            // Hapus kotak lain yang tumpang tindih terlalu banyak (IoU > 0.45) dengan kotak terbaik
            val iterator = boxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                if (calculateIoU(bestBox.boundingBox, nextBox.boundingBox) > IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }
        return finalBoxes
    }

    // Menghitung Intersection over Union (Seberapa nempel kotak A dan B)
    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xA = maxOf(boxA.left, boxB.left)
        val yA = maxOf(boxA.top, boxB.top)
        val xB = minOf(boxA.right, boxB.right)
        val yB = minOf(boxA.bottom, boxB.bottom)

        val intersectionArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val boxAArea = boxA.width() * boxA.height()
        val boxBArea = boxB.width() * boxB.height()

        return intersectionArea / (boxAArea + boxBArea - intersectionArea)
    }

    // Tutup interpreter biar hemat memori
    fun close() {
        interpreter.close()
    }
}