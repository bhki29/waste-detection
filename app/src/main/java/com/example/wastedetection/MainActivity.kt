package com.example.wastedetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Tombol
        val btnVolume = findViewById<Button>(R.id.btnVolumeEstimation)
        val btnDetection = findViewById<Button>(R.id.btnWasteDetection)

        // Aksi Tombol 1: Masuk ke Fitur Estimasi Volume
        btnVolume.setOnClickListener {
            val intent = Intent(this, VolumeCameraActivity::class.java)
            startActivity(intent)
        }

        // Aksi Tombol 2: Placeholder (Karena kita belum buat fitur YOLO-nya)
        btnDetection.setOnClickListener {
            startActivity(Intent(this, DetectionActivity::class.java))
        }
    }
}