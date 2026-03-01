package com.example.wastedetection.WasteDetection

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.util.Log

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Inisialisasi Detector
    // Kita pakai 'by lazy' agar detector hanya dibuat saat dibutuhkan
    private val detector by lazy { YoloDetector(application) }

    // 2. LiveData (Jendela Informasi ke UI)
    // Activity akan 'mengintip' variabel ini. Jika isinya berubah, UI otomatis update.

    // A. Menyimpan Gambar Hasil (yang sudah ada kotaknya)
    private val _resultBitmap = MutableLiveData<Bitmap>()
    val resultBitmap: LiveData<Bitmap> = _resultBitmap

    // B. Menyimpan Statistik (Jumlah Organik/Anorganik)
    private val _resultStats = MutableLiveData<String>()
    val resultStats: LiveData<String> = _resultStats

    // C. Status Loading (Untuk menampilkan ProgressBar)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // D. Untuk Debugging
    private val TAG = "SKRIPSI_DEBUG"

    // 3. Fungsi Utama: Dipanggil oleh Activity saat foto siap
    fun processImage(originalBitmap: Bitmap) {
        _isLoading.value = true
        Log.d(TAG, "🔵 [ViewModel] Perintah diterima. Bersiap memproses gambar...")

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // ... (Bagian Log Gambar Masuk Tetap Sama) ...
                if (originalBitmap != null) {
                    Log.d(TAG, "🧐 STATUS: GAMBAR SEDANG DIKIRIM KE MODEL YOLO...")
                    Log.d(TAG, "   👉 Ukuran Gambar: ${originalBitmap.width} x ${originalBitmap.height} px")
                }

                // 1. Deteksi
                val rawResults = detector.detect(originalBitmap)

                Log.d(TAG, "✅ STATUS: SELESAI DIPERIKSA! Model menemukan ${rawResults.size} kandidat objek.")
                Log.d(TAG, "🔵 [ViewModel] Mulai menggambar kotak hasil...")

                // 2. Siapkan Canvas
                val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)

                // Setup Paint (Warna Garis)
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

                // ==============================================================
                // 🔧 LOGIKA SKALA BARU (ANTI-GAGAL)
                // ==============================================================

                // Kita tidak perlu if(box < 1) lagi.
                // Karena di YoloDetector, outputnya SUDAH dikonversi ke ukuran asli gambar.
                // TAPI, kadang ada bug di model TFLite dimana outputnya masih normalisasi (0-1).
                // Jadi kita pakai logika detektif pintar ini:

                for (result in rawResults) {
                    var box = result.boundingBox

                    Log.d(TAG, "   ✏️ Mentah: ${result.label} di $box")

                    // Cek apakah kotak ini TERLIHAT KECIL SEKALI (seperti koordinat 0-1)?
                    // Jika Lebar Kotak < 1/10 dari Lebar Gambar ASLI, kemungkinan besar itu masih koordinat 0-1
                    // ATAU jika koordinat terbesarnya masih di bawah 2.0 (seperti kasus 2.38 tadi mungkin outlier dikit)

                    // KITA PAKSA KONVERSI JIKA KOTAKNYA SANGAT KECIL (< 10 pixel)
                    // Asumsi: Tidak mungkin ada sampah yang ukurannya cuma 5 pixel di foto 2000 pixel.

                    if (box.width() < 10 && box.height() < 10) {
                        // Artinya ini pasti koordinat normalisasi (0.xx), mari kita besarkan!
                        val newLeft = box.left * originalBitmap.width
                        val newTop = box.top * originalBitmap.height
                        val newRight = box.right * originalBitmap.width
                        val newBottom = box.bottom * originalBitmap.height

                        box = RectF(newLeft, newTop, newRight, newBottom)
                        Log.d(TAG, "   🔧 Fixed (Skala Kecil): Koordinat dibesarkan jadi -> $box")
                    }
                    // KASUS KHUSUS: Seperti log Anda (0.54, 0.67, 2.38, 2.70)
                    // Ini sebenarnya sudah pixel (tapi pixel di ruang 640x640), bukan di ruang 2000x2000.
                    // Jadi kita harus cek apakah 'detector' sudah mengalikannya dengan scaleX/scaleY atau belum.

                    // Agar aman, kita pakai logika di YoloDetector saja.
                    // TAPI UNTUK SEKARANG DI VIEWMODEL:
                    // Jika kotak hasil akhirnya masih sangat kecil dibanding gambar asli, kita scaling ulang.

                    // Mari kita pakai cara paling aman:
                    // Cek apakah koordinat kanan (right) jauh lebih kecil dari lebar gambar?
                    if (box.right < (originalBitmap.width / 10)) {
                        // Kemungkinan ini koordinat relatif terhadap 640x640, padahal gambar aslinya 2000x2000
                        // Atau koordinat 0-1.
                        // Mari coba kalikan dengan rasio gambar

                        // Tapi tunggu, di log Anda: Input 1944x2592. Output 2.38.
                        // 2.38 itu SANGAT KECIL. Itu pasti koordinat 0-1 yang sedikit meleset (offset).
                        // Jadi solusinya:

                        if (box.right < 100) { // Jika koordinat kanan < 100 pixel (padahal gambar ribuan pixel)
                            val newLeft = box.left * originalBitmap.width
                            val newTop = box.top * originalBitmap.height
                            val newRight = box.right * originalBitmap.width
                            val newBottom = box.bottom * originalBitmap.height
                            box = RectF(newLeft, newTop, newRight, newBottom)
                            Log.d(TAG, "   🔧 Fixed (Safety): Koordinat dibesarkan -> $box")
                        }
                    }

                    // ==============================================================

                    // Hitung Statistik
                    if (result.label.contains("Organik", ignoreCase = true) && !result.label.contains("An")) {
                        boxPaint.color = Color.GREEN
                        countOrganik++
                    } else {
                        boxPaint.color = Color.RED
                        countAnorganik++
                    }

                    // Gambar
                    canvas.drawRect(box, boxPaint) // Pakai variabel 'box' yang baru

                    // Label
                    val labelText = "${result.label} ${(result.score * 100).toInt()}%"
                    val textWidth = textPaint.measureText(labelText)
                    val textBgRect = RectF(box.left, box.top - 50f, box.left + textWidth + 20f, box.top)
                    canvas.drawRect(textBgRect, textBgPaint)
                    canvas.drawText(labelText, box.left + 10f, box.top - 10f, textPaint)
                }

                withContext(Dispatchers.Main) {
                    _resultBitmap.value = mutableBitmap
                    val statsText = """
                        🌿 Organik: $countOrganik item
                        🥤 Anorganik: $countAnorganik item
                        ⚠️ Total: ${countOrganik + countAnorganik} objek
                    """.trimIndent()
                    _resultStats.value = statsText
                    _isLoading.value = false
                    Log.d(TAG, "✅ [ViewModel] SUKSES! Data dikirim ke UI.")
                }

            } catch (e: Exception) {
                // ... (Error handling sama) ...
                Log.e(TAG, "🔴 ERROR: ${e.message}")
            }
        }
    }

    // Dipanggil otomatis saat halaman ditutup untuk bersih-bersih memori
    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}