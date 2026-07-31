package com.example.wastedetection.WasteVolumeEstimation

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Class ini menggunakan @RunWith(AndroidJUnit4::class) yang artinya
 * pengujian ini akan dijalankan langsung di dalam sistem Android (HP/Emulator),
 * bukan sekadar di laptop. Ini wajib karena kita butuh Context dan Bitmap.
 */
@RunWith(AndroidJUnit4::class)
class WasteVolumeEstimatorTest {

    // ==============================================================
    // PENGUJIAN 1: INISIALISASI MODEL TENSORFLOW LITE
    // ==============================================================
    @Test
    fun testModelInitialization_doesNotCrash() {
        // 1. Ambil "Context" palsu dari sistem penguji (ibarat meminjam HP)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // 2. Coba buat objek Estimator (Ini akan memicu blok 'init' di kode Anda yang memuat file .tflite)
        var estimator: WasteVolumeEstimator? = null
        try {
            estimator = WasteVolumeEstimator(appContext)
        } catch (e: Exception) {
            // Jika masuk ke sini, berarti model gagal dimuat atau nama file salah
            e.printStackTrace()
        }

        // 3. Ekspektasi: Objek estimator berhasil terbuat dan tidak bernilai 'null'
        assertNotNull("Estimator gagal diinisialisasi, pastikan file .tflite ada di folder assets", estimator)
    }

    // ==============================================================
    // PENGUJIAN 2: ALUR PREDIKSI GAMBAR (INFERENCE)
    // ==============================================================
    @Test
    fun testPrediction_withDummyImage_returnsValidResultFormat() {
        // 1. Persiapan Context dan Estimator
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val estimator = WasteVolumeEstimator(appContext)

        // 2. Buat "Gambar Tiruan" (Dummy Bitmap)
        // membuat kotak hitam polos berukuran 128x128 piksel
        // Ukuran ini menyesuaikan dengan INPUT_SIZE di kode asli Anda
        val dummyBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        dummyBitmap.eraseColor(Color.BLACK)

        // 3. Jalankan fungsi predict() milik Anda menggunakan gambar tiruan tersebut
        val result = estimator.predict(dummyBitmap)

        // 4. Ekspektasi (Assertions):
        // Memastikan hasil tidak kosong
        assertNotNull("Hasil prediksi tidak boleh null", result)

        // Memastikan label yang keluar berwujud teks (misal: "0 %", "10 %", atau "Unknown")
        assertNotNull("Label volume tidak boleh null", result.volumeLabel)

        // Memastikan nilai probabilitas (confidence) masuk akal, yaitu di antara 0.0 hingga 1.0 (0% - 100%)
        assertTrue("Confidence score harus berada di antara 0.0 dan 1.0", result.confidence in 0f..1f)

        // Catatan: Karena gambarnya kotak hitam polos, kita tidak peduli AI menjawab apa.
        // Yang kita uji di sini adalah sistem pra-pemrosesan (resize/normalize) dan sistem
        // pasca-pemrosesan (getMaxResult) Anda BISA BERJALAN TANPA FORCE CLOSE.
    }
}