package com.example.wastedetection

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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.wastedetection.WasteDetection.ScanViewModel
import com.example.wastedetection.ui.FeedbackBottomSheet
import com.google.android.material.button.MaterialButton
import java.io.IOException

class DetectionResultActivity : AppCompatActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_result)

        // 1. Hubungkan variabel
        val imgResult = findViewById<ImageView>(R.id.imgResult)
        val tvOrganikCount = findViewById<TextView>(R.id.tvOrganikCount)
        val tvAnorganikCount = findViewById<TextView>(R.id.tvAnorganikCount)
        val tvTotalCount = findViewById<TextView>(R.id.tvTotalCount)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val btnBackResult = findViewById<ImageView>(R.id.btnBackResult)
        val btnScanAgain = findViewById<MaterialButton>(R.id.btnScanAgain)
        val btnHome = findViewById<MaterialButton>(R.id.btnHome)

        // INISIALISASI KOMPONEN FEEDBACK BARU
        val layoutFeedbackPrompt = findViewById<LinearLayout>(R.id.layoutFeedbackPrompt)
        val btnFeedbackBenar = findViewById<MaterialButton>(R.id.btnFeedbackBenar)
        val btnFeedbackTidak = findViewById<MaterialButton>(R.id.btnFeedbackTidak)


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

            // MUNCULKAN KOTAK PERTANYAAN SETELAH LOADING SELESAI
            layoutFeedbackPrompt.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // 4. Aksi Tombol-Tombol Navigasi
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
            // Ambil gambar hasil deteksi YOLO secara otomatis dari layar
            val imageUri = getUriFromImageView(imgResult)

            if (imageUri != null) {
                // Panggil Modal BottomSheet dengan identitas YOLO
                val modal = FeedbackBottomSheet.newInstance("Deteksi Jenis Sampah YOLO", imageUri.toString())
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
            val file = java.io.File(cacheDir, "auto_capture_yolo_${System.currentTimeMillis()}.jpg")
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