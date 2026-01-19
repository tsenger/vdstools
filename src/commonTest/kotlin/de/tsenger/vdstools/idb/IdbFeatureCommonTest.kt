package de.tsenger.vdstools.idb

import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.FeatureValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@OptIn(ExperimentalStdlibApi::class)
class IdbFeatureCommonTest {

    @Test
    fun testIdbFeatureConstructor() {
        val bytes = "a0a1a2a3a4a5".hexToByteArray()
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.UTF8_STRING, FeatureValue.BytesValue(bytes))
        assertNotNull(feature)
        assertEquals(0x09, feature.tag)
        assertEquals("CAN", feature.name)
        assertEquals(FeatureCoding.UTF8_STRING, feature.coding)
    }

    @Test
    fun testIdbFeatureToString() {
        val bytes = "TestValue".encodeToByteArray()
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.UTF8_STRING, FeatureValue.StringValue("TestValue", bytes))
        assertEquals("CAN: TestValue", feature.toString())
    }

    @Test
    fun testIdbFeatureEncoded() {
        val bytes = "a0a1a2a3".hexToByteArray()
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.BYTES, FeatureValue.BytesValue(bytes))
        // DerTlv encoding: tag (0x09) + length (0x04) + value
        assertEquals("0904a0a1a2a3", feature.encoded.toHexString())
    }

    @Test
    fun testIdbFeatureFromMessageGroup() {
        // Create IdbMessageGroup and get IdbFeature from it
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("CAN", "654321")
            .build()

        val feature = messageGroup.getFeature("CAN")
        assertNotNull(feature)
        assertEquals("CAN", feature.name)
        assertTrue(feature.value is FeatureValue.StringValue)
        assertEquals("654321", feature.value.toString())
    }

    @Test
    fun testIdbFeatureValueAccess() {
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("CAN", "123456")
            .build()

        val feature = messageGroup.featureList[0]
        assertNotNull(feature)
        assertEquals("CAN", feature.name)
        assertEquals(0x09, feature.tag)
    }
}
