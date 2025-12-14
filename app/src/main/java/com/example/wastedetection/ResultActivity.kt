package com.example.wastedetection

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.load
import java.io.InputStream

class ResultActivity : AppCompatActivity() {

    // Panggil ViewModel yang sudah kita buat di Step 3
    private val viewModel: VolumeEstimationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 1. Inisialisasi UI
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvPercentage = findViewById<TextView>(R.id.tvPercentage)
        val tvConfidence = findViewById<TextView>(R.id.tvConfidence)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 2. Ambil Data URI Gambar yang dikirim dari CameraActivity
        val imageUriString = intent.getStringExtra("image_uri")

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)

            // Tampilkan gambar ke layar menggunakan Coil (Cepat & Ringan)
            imgResult.load(imageUri)

            // 3. Konversi URI ke Bitmap untuk dianalisis AI
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // KIRIM KE AI (ViewModel)
                if (bitmap != null) {
                    tvPercentage.text = "Menganalisis..."
                    viewModel.detectVolume(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Observasi Hasil AI (Update UI saat hasil keluar)
        viewModel.predictionResult.observe(this) { resultLabel ->
            tvPercentage.text = resultLabel // Contoh: "70 %"
        }

        viewModel.confidenceScore.observe(this) { confidence ->
            tvConfidence.text = confidence // Contoh: "Akurasi: 95%"
        }

        // 5. Tombol Back & Save
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            Toast.makeText(this, "Fitur simpan belum aktif", Toast.LENGTH_SHORT).show()
        }
    }
}