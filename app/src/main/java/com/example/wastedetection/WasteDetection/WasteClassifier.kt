package com.example.wastedetection.WasteDetection

/**
 * Class mandiri ini murni hanya berisi logika pemilah teks.
 * Karena tidak ada elemen Android (Canvas/Color) di sini, class ini 100% bisa di-Local Unit Test!
 */
class WasteClassifier {

    // Fungsi ini mengembalikan TRUE jika Organik, dan FALSE jika Anorganik
    fun isOrganik(label: String): Boolean {
        // Ini adalah murni logika asli Anda yang kita pindahkan ke sini
        return label.contains("Organik", ignoreCase = true) && !label.contains("An")
    }

}