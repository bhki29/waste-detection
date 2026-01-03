package com.example.wastedetection

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.load
import java.io.InputStream

class ResultActivity : AppCompatActivity() {

    private val viewModel: VolumeEstimationViewModel by viewModels()

    // Variabel untuk menyimpan persentase dari AI (0-100)
    private var detectedPercentage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 1. Inisialisasi Komponen UI
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvRawPercentage = findViewById<TextView>(R.id.tvRawPercentage)
        val tvConfidence = findViewById<TextView>(R.id.tvConfidence)
        val etContainerVolume = findViewById<EditText>(R.id.etContainerVolume)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val layoutFinalResult = findViewById<LinearLayout>(R.id.layoutFinalResult)
        val tvFinalVolume = findViewById<TextView>(R.id.tvFinalVolume)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // 2. Ambil Gambar dari Intent
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgResult.load(imageUri) // Tampilkan gambar

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
            // resultLabel formatnya "70 %" -> Kita perlu ambil angkanya saja
            tvRawPercentage.text = "Tingkat Kepenuhan: $resultLabel"

            // Ambil angka dari string (misal "70 %" jadi 70)
            val cleanString = resultLabel.replace("%", "").trim()
            detectedPercentage = cleanString.toIntOrNull() ?: 0
        }

        viewModel.confidenceScore.observe(this) { confidence ->
            tvConfidence.text = confidence
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
                // RUMUS: (Persentase / 100) * Liter * 1000 = mL
                val volumeInMl = (detectedPercentage.toDouble() / 100.0) * containerVolumeLiter * 1000.0

                // Tampilkan Hasil Akhir
                tvFinalVolume.text = "${volumeInMl.toInt()} mL"
                layoutFinalResult.visibility = View.VISIBLE // Munculkan kotak hasil

                // Scroll ke bawah agar terlihat (Opsional)
            } else {
                Toast.makeText(this, "Input angka tidak valid", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener { finish() }
    }
}