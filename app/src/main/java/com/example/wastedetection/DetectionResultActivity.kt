package com.example.wastedetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.wastedetection.WasteDetection.ScanViewModel
import java.io.IOException

class DetectionResultActivity : AppCompatActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_result)

        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvStats = findViewById<TextView>(R.id.tvStats)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnBack = findViewById<Button>(R.id.btnScanAgain)

        val imageUriString = intent.getStringExtra("image_uri")

        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            try {
                // 1. Ambil Gambar Resolusi Penuh
                val bitmap = uriToBitmap(uri)

                if (bitmap != null) {
                    // Cek di Logcat nanti, ukurannya harus BESAR (> 1000px)
                    viewModel.processImage(bitmap)
                } else {
                    Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Observer (Sama seperti sebelumnya)
        viewModel.resultBitmap.observe(this) { resultImage ->
            imgResult.setImageBitmap(resultImage)
        }

        viewModel.resultStats.observe(this) { stats ->
            tvStats.text = stats
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnBack.isEnabled = !isLoading
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    // =========================================================================
    // 🔧 FUNGSI BARU: MEMAKSA LOAD GAMBAR ASLI (HIGH RES)
    // =========================================================================
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val contentResolver = contentResolver
            var bitmap: Bitmap? = null

            // CARA 1: Untuk Android Versi Baru (Android P / API 28 ke atas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true // Agar bisa digambari kotak
                }
            }
            // CARA 2: Untuk Android Lama
            else {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            // CARA 3: Fallback (Cadangan jika cara diatas gagal)
            if (bitmap == null) {
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            }

            if (bitmap == null) return null

            // --- PROSES ROTASI (Agar Gambar Tegak) ---
            val matrix = Matrix()
            val projection = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
            val cursor = contentResolver.query(uri, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                val orientationCol = cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION)
                if (orientationCol >= 0) {
                    val rotation = cursor.getInt(orientationCol)
                    if (rotation != 0) {
                        matrix.postRotate(rotation.toFloat())
                    }
                }
                cursor.close()
            }

            // Putar gambar & Pastikan Mutable (Bisa diedit)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // Trik pamungkas: Copy ke format ARGB_8888 agar pasti bisa digambari Canvas
            return rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}