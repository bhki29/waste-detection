package com.example.wastedetection.WasteDetection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteClassifierTest {

    // Memanggil class "Otak" yang akan diuji
    private val classifier = WasteClassifier()

    @Test
    fun testIsOrganik_WithValidOrganik_ReturnsTrue() {
        // Skenario Normal: Model mengeluarkan teks "Sampah Organik"
        val result = classifier.isOrganik("Sampah Organik")

        // Ekspektasi: Harus TRUE (Masuk keranjang hijau)
        assertTrue(result)
    }

    @Test
    fun testIsOrganik_WithValidAnorganik_ReturnsFalse() {
        // Skenario Normal: Model mengeluarkan teks "Sampah Anorganik"
        val result = classifier.isOrganik("Sampah Anorganik")

        // Ekspektasi: Harus FALSE. Menguji apakah kata "An" menggagalkan "Organik".
        assertFalse(result)
    }

    @Test
    fun testIsOrganik_WithMessyOrganik_ReturnsTrue() {
        // Skenario Ekstrem: Huruf besar kecil berantakan
        val result = classifier.isOrganik("daun OrGaNiK")
        assertTrue(result)
    }

    @Test
    fun testIsOrganik_WithDeceptiveAnorganik_ReturnsFalse() {
        // Skenario Ekstrem: Teks mengecoh.
        val result = classifier.isOrganik("Botol Plastik An-organik")

        // Ekspektasi: Karena mengandung "An", maka statusnya harus Anorganik (False)
        assertFalse(result)
    }
}