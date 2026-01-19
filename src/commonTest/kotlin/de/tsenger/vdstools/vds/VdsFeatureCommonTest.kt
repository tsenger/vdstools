package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class VdsFeatureCommonTest {

    @Test
    fun testVdsFeature_BYTE_value() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val feature = VdsFeature(127, "FEATURE1", FeatureCoding.BYTE, FeatureValue.fromBytes(bytes, FeatureCoding.BYTE))
        assertTrue(feature.value is FeatureValue.ByteValue)
        assertEquals(127, (feature.value as FeatureValue.ByteValue).value)
        assertEquals("127", feature.value.toString())
    }

    @Test
    fun testVdsFeature_C40_value() {
        val bytes = DataEncoder.encodeC40("DETS32")
        val feature = VdsFeature(2, "FEATURE2", FeatureCoding.C40, FeatureValue.fromBytes(bytes, FeatureCoding.C40))
        assertTrue(feature.value is FeatureValue.StringValue)
        assertEquals("DETS32", (feature.value as FeatureValue.StringValue).value)
        assertEquals("DETS32", feature.value.toString())
    }

    @Test
    fun testVdsFeature_UTF8_value() {
        val bytes = "Jâcob".encodeToByteArray()
        val feature = VdsFeature(3, "FEATURE3", FeatureCoding.UTF8_STRING, FeatureValue.fromBytes(bytes, FeatureCoding.UTF8_STRING))
        assertTrue(feature.value is FeatureValue.StringValue)
        assertEquals("Jâcob", (feature.value as FeatureValue.StringValue).value)
    }

    @Test
    fun testVdsFeature_BYTES_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val feature = VdsFeature(4, "FEATURE4", FeatureCoding.BYTES, FeatureValue.fromBytes(bytes, FeatureCoding.BYTES))
        assertTrue(feature.value is FeatureValue.BytesValue)
        assertEquals("badc0ffe", feature.value.toString())
    }

    @Test
    fun testVdsFeature_BYTE_valueStr() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val feature = VdsFeature(5, "FEATURE5", FeatureCoding.BYTE, FeatureValue.fromBytes(bytes, FeatureCoding.BYTE))
        assertEquals("127", feature.value.toString())
    }

    @Test
    fun testVdsFeature_BYTES_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val feature = VdsFeature(6, "FEATURE6", FeatureCoding.BYTES, FeatureValue.fromBytes(bytes, FeatureCoding.BYTES))
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.value.rawBytes)
    }

    @Test
    fun testVdsFeature_UNKNOWN_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val feature = VdsFeature(6, "FEATURE6", FeatureCoding.UNKNOWN, FeatureValue.fromBytes(bytes, FeatureCoding.UNKNOWN))
        assertContentEquals("BADC0FFE".hexToByteArray(), feature.value.rawBytes)
    }

    @Test
    fun testVdsFeature_UNKNOWN_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val feature = VdsFeature(6, "FEATURE6", FeatureCoding.UNKNOWN, FeatureValue.fromBytes(bytes, FeatureCoding.UNKNOWN))
        assertEquals("badc0ffe", feature.value.toString())
    }

    @Test
    fun testVdsFeature_toString() {
        val bytes = "Test".encodeToByteArray()
        val feature = VdsFeature(1, "MY_FEATURE", FeatureCoding.UTF8_STRING, FeatureValue.fromBytes(bytes, FeatureCoding.UTF8_STRING))
        assertEquals("MY_FEATURE: Test", feature.toString())
    }
}
