package com.example.wastedetection.WasteVolumeEstimation

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.load
import com.example.wastedetection.MainActivity
import com.example.wastedetection.R
import com.example.wastedetection.ui.FeedbackBottomSheet
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.InputStream

class ResultActivity : AppCompatActivity() {

    private val viewModel: VolumeEstimationViewModel by viewModels()
    private val calculator = VolumeCalculator()
    private var detectedPercentage: Double = 0.0

    private val TAG = "PantauEstimasi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        Log.d(TAG, "=== Halaman ResultActivity Dibuka ===")

        val btnBackResult = findViewById<ImageView>(R.id.btnBackResult)
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvRawPercentage = findViewById<TextView>(R.id.tvRawPercentage)
        val tvConfidence = findViewById<TextView>(R.id.tvConfidence)
        val etContainerVolume = findViewById<TextInputEditText>(R.id.etContainerVolume)
        val btnCalculate = findViewById<MaterialButton>(R.id.btnCalculate)
        val layoutFinalResult = findViewById<CardView>(R.id.layoutFinalResult)
        val btnScanAgain = findViewById<MaterialButton>(R.id.btnScanAgain)
        val btnHome = findViewById<MaterialButton>(R.id.btnHome)
        val tvFinalVolume = findViewById<TextView>(R.id.tvFinalVolume)

        // INISIALISASI KOMPONEN FEEDBACK BARU
        val layoutFeedbackPrompt = findViewById<LinearLayout>(R.id.layoutFeedbackPrompt)
        val btnFeedbackBenar = findViewById<MaterialButton>(R.id.btnFeedbackBenar)
        val btnFeedbackTidak = findViewById<MaterialButton>(R.id.btnFeedbackTidak)


        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgResult.load(imageUri)
            Log.d(TAG, "Gambar berhasil dimuat dari URI: $imageUriString")

            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    tvRawPercentage.text = "Menganalisis..."
                    Log.d(TAG, "Memulai proses deteksi TFLite...")
                    viewModel.detectVolume(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat gambar untuk dianalisis: ${e.message}")
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.predictionResult.observe(this) { resultLabel ->
            Log.d(TAG, "Model TFLite mengeluarkan label asli: '$resultLabel'")
            tvRawPercentage.text = "Tingkat Kepenuhan : $resultLabel"
            detectedPercentage = calculator.parsePercentage(resultLabel)
            Log.d(TAG, "Label dibersihkan menjadi angka desimal: $detectedPercentage")
        }

        viewModel.confidenceScore.observe(this) { confidence ->
            Log.d(TAG, "Akurasi (Confidence Score) AI: $confidence")
            tvConfidence.text = "Akurasi : $confidence"
        }

        btnCalculate.setOnClickListener {
            val inputString = etContainerVolume.text.toString()
            Log.d(TAG, "Tombol Hitung ditekan. Input Kapasitas User: '$inputString'")

            if (inputString.isEmpty()) {
                Log.e(TAG, "User menekan tombol tanpa mengisi kapasitas!")
                etContainerVolume.error = "Masukkan kapasitas wadah!"
                return@setOnClickListener
            }

            val containerVolumeLiter = inputString.toDoubleOrNull()

            if (containerVolumeLiter != null) {
                val formattedVolume = calculator.calculateVolume(detectedPercentage, containerVolumeLiter)
                Log.d(TAG, "Kalkulasi Berhasil -> $formattedVolume Liter")

                tvFinalVolume.text = "$formattedVolume Liter"
                layoutFinalResult.visibility = View.VISIBLE

                // MUNCULKAN KOTAK PERTANYAAN FEEDBACK
                layoutFeedbackPrompt.visibility = View.VISIBLE
            } else {
                Log.e(TAG, "Input angka dari user tidak valid (bukan angka).")
                Toast.makeText(this, "Input angka tidak valid", Toast.LENGTH_SHORT).show()
            }
        }

        btnBackResult.setOnClickListener { finish() }
        btnScanAgain.setOnClickListener { finish() }
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // ==========================================
        // LOGIKA TOMBOL BENAR (Tutup & Selesai)
        // ==========================================
        btnFeedbackBenar.setOnClickListener {
            layoutFeedbackPrompt.visibility = View.GONE
            Toast.makeText(this, "Terima kasih atas konfirmasinya!", Toast.LENGTH_SHORT).show()
        }

        // ==========================================
        // LOGIKA TOMBOL TIDAK (Auto-Capture & Kirim)
        // ==========================================
        btnFeedbackTidak.setOnClickListener {
            // Ambil gambar secara otomatis dari layar
            val imageUri = getUriFromImageView(imgResult)

            if (imageUri != null) {
                // INFO: Garis merah pada "newInstance" di bawah ini sangat wajar!
                // Ini karena kita belum merombak file FeedbackBottomSheet.kt
                val modal = FeedbackBottomSheet.newInstance("Estimasi Volume", imageUri.toString())
                modal.show(supportFragmentManager, "FeedbackModal")
            } else {
                Toast.makeText(this, "Gagal mengekstrak gambar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // MESIN AUTO-CAPTURE GAMBAR (DI BALIK LAYAR)
    // ==========================================
    private fun getUriFromImageView(imageView: ImageView): Uri? {
        val drawable = imageView.drawable ?: return null

        val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }

        return try {
            val file = java.io.File(cacheDir, "auto_capture_${System.currentTimeMillis()}.jpg")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()

            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}