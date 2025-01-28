package de.tsenger.vdstools.vds

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class FeatureCommonTest {

    @Test
    fun testFeature_BYTE_valueInt() {
        val feature = Feature("FEATURE1", Byte.MAX_VALUE, FeatureCoding.BYTE)
        assertEquals(127, feature.valueInt)
    }

    @Test
    fun testFeature_C40_valueStr() {
        val feature = Feature("FEATURE2", "DETS32", FeatureCoding.C40)
        assertEquals("DETS32", feature.valueStr)
    }

    @Test
    fun testFeature_UTF8_valueStr() {
        val feature = Feature("FEATURE3", "Jâcob", FeatureCoding.UTF8_STRING)
        assertEquals("Jâcob", feature.valueStr)
    }

    @Test
    fun testFeature_BYTES_valueStr() {
        val feature = Feature("FEATURE4", "BADC0FFE".hexToByteArray(), FeatureCoding.BYTES)
        assertEquals("BADC0FFE", feature.valueStr.uppercase())
    }

    @Test
    fun testFeature_BYTE_valueStr() {
        val feature = Feature("FEATURE5", Byte.MAX_VALUE, FeatureCoding.BYTE)
        assertEquals("127", feature.valueStr)
    }

    @Test
    fun testFeature_BYTES_valueBytes() {
        val feature = Feature("FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.BYTES)
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.valueBytes)
    }

    @Test
    fun testFeature_UNKNOWN_valueBytes() {
        val feature = Feature("FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.UNKNOWN)
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.valueBytes)
    }

    @Test
    fun testFeature_UNKNOWN_valueStr() {
        val feature = Feature("FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.UNKNOWN)
        assertEquals("BADC0FFE", feature.valueStr.uppercase())
    }
}