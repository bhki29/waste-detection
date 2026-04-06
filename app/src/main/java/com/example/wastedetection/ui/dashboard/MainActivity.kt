package com.example.wastedetection.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.wastedetection.R
import com.example.wastedetection.WasteDetection.CameraActivity
import com.example.wastedetection.WasteVolumeEstimation.VolumeCameraActivity

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
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}