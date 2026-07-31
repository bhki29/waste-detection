package com.example.wastedetection.WasteVolumeEstimation

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeCalculatorTest {

    private val calculator = VolumeCalculator()

    @Test
    fun testParsePercentage_CorrectInput() {
        val result = calculator.parsePercentage("75 %")
        assertEquals(75.0, result, 0.0)
    }

    @Test
    fun testParsePercentage_MessyInput() {
        val result = calculator.parsePercentage("  100%  ")
        assertEquals(100.0, result, 0.0)
    }

    @Test
    fun testCalculateVolume_NormalCase() {
        // AI 50%, Wadah 10 Liter -> Hasil: 5.00
        val result = calculator.calculateVolume(50.0, 10.0)
        assertEquals("5.00", result)
    }

    @Test
    fun testCalculateVolume_DecimalCase() {
        // AI 33%, Wadah 10 Liter -> Hasil: 3.30
        val result = calculator.calculateVolume(33.0, 10.0)
        assertEquals("3.30", result)
    }

    @Test
    fun testCalculateVolume_RoundingUp() {
        // AI 87%, Wadah 1.5 Liter -> 1.305 dibulatkan menjadi 1.31
        val result = calculator.calculateVolume(87.0, 1.5)
        assertEquals("1.31", result)
    }
}