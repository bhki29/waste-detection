package com.example.wastedetection.WasteVolumeEstimation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.example.wastedetection.R
import com.google.android.material.button.MaterialButton
import java.io.File

class VolumeCameraActivity : AppCompatActivity() {

    // --- Referensi Elemen UI (Wajah 1 & Wajah 2) ---
    private lateinit var llPlaceholderState: LinearLayout
    private lateinit var cvPreviewState: CardView
    private lateinit var ivPreview: ImageView
    private lateinit var llActionPick: LinearLayout
    private lateinit var llActionConfirm: LinearLayout

    // Variabel untuk menyimpan lokasi (URI) gambar sementara
    private var currentImageUri: Uri? = null

    // ==========================================
    // LAUNCHER: MENANGKAP HASIL DARI KAMERA/GALERI
    // ==========================================

    // 1. Launcher Kamera
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uriAman = currentImageUri

        if (success && uriAman != null) {
            // Jika berhasil difoto, ubah tampilan ke Wajah 2 (Preview)
            showPreviewState(uriAman)
        } else {
            Toast.makeText(this, "Dibatalkan atau gagal mengambil foto", Toast.LENGTH_SHORT).show()
            currentImageUri = null
        }
    }

    // 2. Launcher Galeri
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Jika gambar dipilih dari galeri, simpan URI dan ubah ke Wajah 2
            currentImageUri = uri
            showPreviewState(uri)
        }
    }

    // ==========================================
    // FUNGSI UTAMA (ON CREATE)
    // ==========================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume_camera)

        // Hubungkan variabel dengan ID di XML
        llPlaceholderState = findViewById(R.id.llPlaceholderState)
        cvPreviewState = findViewById(R.id.cvPreviewState)
        ivPreview = findViewById(R.id.ivPreview)
        llActionPick = findViewById(R.id.llActionPick)
        llActionConfirm = findViewById(R.id.llActionConfirm)

        val btnKamera = findViewById<MaterialButton>(R.id.btnKamera)
        val btnGaleri = findViewById<MaterialButton>(R.id.btnGaleri)
        val btnKlasifikasi = findViewById<MaterialButton>(R.id.btnKlasifikasi)
        val btnGantiGambar = findViewById<MaterialButton>(R.id.btnGantiGambar)
        val btnBackHeader = findViewById<ImageView>(R.id.btnBackHeader)

        // 1. Tombol Kembali di Pojok Kiri Atas
        btnBackHeader.setOnClickListener {
            finish() // Menutup halaman dan kembali ke Home
        }

        // 2. Tombol Kamera (Wajah 1)
        btnKamera.setOnClickListener {
            openCamera()
        }

        // 3. Tombol Galeri (Wajah 1)
        btnGaleri.setOnClickListener {
            galleryLauncher.launch("image/*") // Membuka file explorer khusus gambar
        }

        // 4. Tombol Ganti Gambar (Wajah 2)
        btnGantiGambar.setOnClickListener {
            showPlaceholderState() // Mengembalikan tampilan ke Wajah 1
        }

        // 5. Tombol Klasifikasi Sekarang (Wajah 2)
        btnKlasifikasi.setOnClickListener {
            val uriAman = currentImageUri

            if (uriAman != null) {
                // Pindah ke Halaman Hasil Estimasi dan bawa data URI Gambar-nya
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("image_uri", uriAman.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Terjadi kesalahan, gambar tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // FUNGSI PENDUKUNG
    // ==========================================

    // Membuka Kamera dan menyimpan di ruang rahasia (Cache) tanpa perlu izin
    private fun openCamera() {
        try {
            // 1. Buat file sementara di folder cache aplikasi
            val photoFile = File(cacheDir, "foto_estimasi_${System.currentTimeMillis()}.jpg")

            // 2. Dapatkan URI aman menggunakan FileProvider
            currentImageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            val uriAman = currentImageUri

            if (uriAman != null) {
                // 3. Buka kamera
                cameraLauncher.launch(uriAman)
            } else {
                Toast.makeText(this, "Gagal menyiapkan URI kamera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error Kamera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- MANAJEMEN TAMPILAN WAJAH 1 & WAJAH 2 ---

    // Menampilkan Wajah 1 (Kondisi Awal / Ganti Gambar)
    private fun showPlaceholderState() {
        currentImageUri = null // Hapus memori gambar lama

        // Munculkan Placeholder & Tombol Pilih
        llPlaceholderState.visibility = View.VISIBLE
        llActionPick.visibility = View.VISIBLE

        // Sembunyikan Preview & Tombol Konfirmasi
        cvPreviewState.visibility = View.GONE
        llActionConfirm.visibility = View.GONE
    }

    // Menampilkan Wajah 2 (Kondisi Preview Foto)
    private fun showPreviewState(uri: Uri) {
        ivPreview.setImageURI(uri) // Pasang gambar ke layar

        // Sembunyikan Placeholder & Tombol Pilih
        llPlaceholderState.visibility = View.GONE
        llActionPick.visibility = View.GONE

        // Munculkan Preview & Tombol Konfirmasi
        cvPreviewState.visibility = View.VISIBLE
        llActionConfirm.visibility = View.VISIBLE
    }
}