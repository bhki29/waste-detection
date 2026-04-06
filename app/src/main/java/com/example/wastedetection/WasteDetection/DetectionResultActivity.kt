package com.example.wastedetection.WasteDetection

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.wastedetection.ui.dashboard.MainActivity
import com.example.wastedetection.R
import com.google.android.material.button.MaterialButton
import java.io.IOException

class DetectionResultActivity : AppCompatActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_result)

        // 1. Hubungkan variabel dengan ID yang baru di XML
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvOrganikCount = findViewById<TextView>(R.id.tvOrganikCount)
        val tvAnorganikCount = findViewById<TextView>(R.id.tvAnorganikCount)
        val tvTotalCount = findViewById<TextView>(R.id.tvTotalCount)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val btnBackResult = findViewById<ImageView>(R.id.btnBackResult)
        val btnScanAgain = findViewById<MaterialButton>(R.id.btnScanAgain)
        val btnHome = findViewById<MaterialButton>(R.id.btnHome)

        // 2. Terima dan Proses Gambar
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            try {
                val bitmap = uriToBitmap(uri)
                if (bitmap != null) {
                    viewModel.processImage(bitmap)
                } else {
                    Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Observer: Menampilkan Hasil ke Layar
        viewModel.resultBitmap.observe(this) { resultImage ->
            imgResult.setImageBitmap(resultImage)
        }

        viewModel.organikCount.observe(this) { count ->
            tvOrganikCount.text = "Organik: $count item"
        }

        viewModel.anorganikCount.observe(this) { count ->
            tvAnorganikCount.text = "Anorganik: $count item"
        }

        viewModel.totalCount.observe(this) { count ->
            tvTotalCount.text = "Total: $count objek"
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnScanAgain.isEnabled = !isLoading
            btnHome.isEnabled = !isLoading
        }

        // 4. Aksi Tombol-Tombol
        // Tombol Back Kiri Atas (Tutup halaman ini, kembali ke kamera)
        btnBackResult.setOnClickListener {
            finish()
        }

        // Tombol Ulangi (Tutup halaman ini, kembali ke kamera)
        btnScanAgain.setOnClickListener {
            finish()
        }

        // Tombol Home (Tutup semua halaman kamera/hasil, paksa kembali ke MainActivity)
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Trik agar MainActivity tidak ditumpuk, tapi halaman lain dibersihkan
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    // =========================================================================
    // FUNGSI MEMAKSA LOAD GAMBAR ASLI (HIGH RES) - (TIDAK ADA YANG DIUBAH)
    // =========================================================================
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val contentResolver = contentResolver
            var bitmap: Bitmap? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            if (bitmap == null) {
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            }

            if (bitmap == null) return null

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

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            return rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}