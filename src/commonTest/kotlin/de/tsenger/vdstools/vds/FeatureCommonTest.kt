package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class FeatureCommonTest {

    @Test
    fun testFeature_BYTE_valueInt() {
        val feature = Feature(127, "FEATURE1", byteArrayOf(Byte.MAX_VALUE), FeatureCoding.BYTE)
        assertEquals(127, feature.valueInt)
    }

    @Test
    fun testFeature_C40_valueStr() {
        val feature = Feature(2, "FEATURE2", DataEncoder.encodeC40("DETS32"), FeatureCoding.C40)
        assertEquals("DETS32", feature.valueStr)
    }

    @Test
    fun testFeature_UTF8_valueStr() {
        val feature = Feature(3, "FEATURE3", "Jâcob".encodeToByteArray(), FeatureCoding.UTF8_STRING)
        assertEquals("Jâcob", feature.valueStr)
    }

    @Test
    fun testFeature_BYTES_valueStr() {
        val feature = Feature(4, "FEATURE4", "BADC0FFE".hexToByteArray(), FeatureCoding.BYTES)
        assertEquals("BADC0FFE", feature.valueStr.uppercase())
    }

    @Test
    fun testFeature_BYTE_valueStr() {
        val feature = Feature(5, "FEATURE5", byteArrayOf(Byte.MAX_VALUE), FeatureCoding.BYTE)
        assertEquals("127", feature.valueStr)
    }

    @Test
    fun testFeature_BYTES_valueBytes() {
        val feature = Feature(6, "FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.BYTES)
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.valueBytes)
    }

    @Test
    fun testFeature_UNKNOWN_valueBytes() {
        val feature = Feature(6, "FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.UNKNOWN)
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.valueBytes)
    }

    @Test
    fun testFeature_UNKNOWN_valueStr() {
        val feature = Feature(6, "FEATURE6", "BADC0FFE".hexToByteArray(), FeatureCoding.UNKNOWN)
        assertEquals("BADC0FFE", feature.valueStr.uppercase())
    }
}