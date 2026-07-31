package com.example.wastedetection.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.wastedetection.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class FeedbackBottomSheet : BottomSheetDialogFragment() {

    private lateinit var etFeedbackMessage: TextInputEditText
    private var fiturAsal: String = ""
    private var imageUriString: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_feedback_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Membaca kiriman data dari ResultActivity
        fiturAsal = arguments?.getString("FITUR_ASAL") ?: ""
        imageUriString = arguments?.getString("IMAGE_URI")

        etFeedbackMessage = view.findViewById(R.id.etFeedbackMessage)
        val btnSendFeedback = view.findViewById<MaterialButton>(R.id.btnSendFeedback)

        btnSendFeedback.setOnClickListener {
            val userJawaban = etFeedbackMessage.text.toString().trim()

            // Validasi input kosong
            if (userJawaban.isEmpty()) {
                etFeedbackMessage.error = "Jawaban tidak boleh kosong!"
                return@setOnClickListener
            }

            if (imageUriString == null) {
                Toast.makeText(requireContext(), "Gambar dataset tidak ditemukan.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imageUri = Uri.parse(imageUriString)

            // Merakit Intent Email dengan 1 Lampiran Gambar Otomatis
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg" // Berubah menjadi tipe image karena membawa data file tunggal

                // Tujuan terkunci ke email universitas Anda
                putExtra(Intent.EXTRA_EMAIL, arrayOf("2203010042@unper.ac.id"))

                // Subjek email otomatis bersifay dinamis
                putExtra(Intent.EXTRA_SUBJECT, "Koreksi Dataset - $fiturAsal")

                // Isi email terstruktur
                val emailBody = "Laporan Koreksi Pengguna:\n\n" +
                        "Fitur dari : $fiturAsal\n" +
                        "Koreksi / Label Seharusnya: $userJawaban\n\n" +
                        "--\nDikirim otomatis dari aplikasi GreenScan AI."
                putExtra(Intent.EXTRA_TEXT, emailBody)

                // Menyisipkan URI gambar hasil capture layar secara langsung
                putExtra(Intent.EXTRA_STREAM, imageUri)

                // Memberikan hak akses sementara bagi Gmail untuk membaca file di cache
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Kirim Koreksi via Email..."))
                dismiss() // Menutup dialog setelah intent sukses berpindah ke Gmail
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Aplikasi Email tidak ditemukan di HP ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        // Pembaruan fungsi: Menerima 2 argumen untuk mengakomodasi transfer URI gambar otomatis
        fun newInstance(fiturAsal: String, imageUri: String): FeedbackBottomSheet {
            val fragment = FeedbackBottomSheet()
            val args = Bundle().apply {
                putString("FITUR_ASAL", fiturAsal)
                putString("IMAGE_URI", imageUri)
            }
            fragment.arguments = args
            return fragment
        }
    }
}