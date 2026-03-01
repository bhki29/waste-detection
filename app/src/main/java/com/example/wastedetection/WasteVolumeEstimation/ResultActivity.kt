package com.example.wastedetection.WasteVolumeEstimation

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.load
import com.example.wastedetection.MainActivity
import com.example.wastedetection.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.InputStream

class ResultActivity : AppCompatActivity() {

    private val viewModel: VolumeEstimationViewModel by viewModels()

    // Variabel untuk menyimpan persentase dari AI (0-100)
    private var detectedPercentage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result) // Pastikan nama file XML-nya benar

        // 1. Inisialisasi Komponen UI (Disesuaikan dengan ID XML yang baru)
        val btnBackResult = findViewById<ImageView>(R.id.btnBackResult)
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvRawPercentage = findViewById<TextView>(R.id.tvRawPercentage)
        val tvConfidence = findViewById<TextView>(R.id.tvConfidence)
        val etContainerVolume = findViewById<TextInputEditText>(R.id.etContainerVolume)
        val btnCalculate = findViewById<MaterialButton>(R.id.btnCalculate)
        val layoutFinalResult = findViewById<CardView>(R.id.layoutFinalResult) // Diubah jadi CardView
        val tvFinalVolume = findViewById<TextView>(R.id.tvFinalVolume)
        val btnScanAgain = findViewById<MaterialButton>(R.id.btnScanAgain)
        val btnHome = findViewById<MaterialButton>(R.id.btnHome)

        // 2. Ambil Gambar dari Intent
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgResult.load(imageUri) // Tampilkan gambar pakai library Coil

            // Konversi ke Bitmap & Kirim ke AI
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    tvRawPercentage.text = "Menganalisis..."
                    viewModel.detectVolume(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Observasi Hasil AI (Dapatkan Persentase)
        viewModel.predictionResult.observe(this) { resultLabel ->
            // Menyesuaikan teks dengan desain Figma
            tvRawPercentage.text = "Tingkat Kepenuhan : $resultLabel"

            // Ambil angka dari string (misal "70 %" jadi 70)
            val cleanString = resultLabel.replace("%", "").trim()
            detectedPercentage = cleanString.toIntOrNull() ?: 0
        }

        viewModel.confidenceScore.observe(this) { confidence ->
            tvConfidence.text = confidence // "Akurasi : 99 %"
        }

        // 4. Logika Tombol HITUNG
        btnCalculate.setOnClickListener {
            // Ambil input dari user (Liter)
            val inputString = etContainerVolume.text.toString()

            if (inputString.isEmpty()) {
                etContainerVolume.error = "Masukkan kapasitas wadah!"
                return@setOnClickListener
            }

            val containerVolumeLiter = inputString.toDoubleOrNull()

            if (containerVolumeLiter != null) {
                // RUMUS: (Persentase / 100) * Liter * 1000 = mL / CM3
                val volumeInMl = (detectedPercentage.toDouble() / 100.0) * containerVolumeLiter * 1000.0

                // Tampilkan Hasil Akhir di dalam Kotak Hijau
                tvFinalVolume.text = "${volumeInMl.toInt()} CM"
                layoutFinalResult.visibility = View.VISIBLE // Munculkan kotak hasil
            } else {
                Toast.makeText(this, "Input angka tidak valid", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Logika Tombol Navigasi
        // Tombol kembali di header (tutup halaman ini)
        btnBackResult.setOnClickListener {
            finish()
        }

        // Tombol Ulangi di bawah (tutup halaman ini, kembali ke kamera)
        btnScanAgain.setOnClickListener {
            finish()
        }

        // Tombol Home (Bersihkan history layar dan kembali ke menu utama)
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}