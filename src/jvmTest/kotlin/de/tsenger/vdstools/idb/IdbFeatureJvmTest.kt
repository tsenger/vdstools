package de.tsenger.vdstools.idb

import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.FeatureValue
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class IdbFeatureJvmTest {

    @Test
    fun testIdbFeatureConstructor() {
        val bytes = Hex.decode("a0a1a2a3a4a5")
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.UTF8_STRING, FeatureValue.BytesValue(bytes))
        Assert.assertNotNull(feature)
        Assert.assertEquals(0x09, feature.tag)
        Assert.assertEquals("CAN", feature.name)
        Assert.assertEquals(FeatureCoding.UTF8_STRING, feature.coding)
    }

    @Test
    fun testIdbFeatureToString() {
        val bytes = "TestValue".encodeToByteArray()
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.UTF8_STRING, FeatureValue.StringValue("TestValue", bytes))
        Assert.assertEquals("CAN: TestValue", feature.toString())
    }

    @Test
    fun testIdbFeatureEncoded() {
        val bytes = Hex.decode("a0a1a2a3")
        val feature = IdbFeature(0x09, "CAN", FeatureCoding.BYTES, FeatureValue.BytesValue(bytes))
        // DerTlv encoding: tag (0x09) + length (0x04) + value
        Assert.assertEquals("0904a0a1a2a3", Hex.toHexString(feature.encoded))
    }

    @Test
    fun testIdbFeatureFromMessageGroup() {
        // Create IdbMessageGroup and get IdbFeature from it
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("CAN", "654321")
            .build()

        val feature = messageGroup.getFeature("CAN")
        Assert.assertNotNull(feature)
        Assert.assertEquals("CAN", feature!!.name)
        Assert.assertTrue(feature.value is FeatureValue.StringValue)
        Assert.assertEquals("654321", feature.value.toString())
    }

    @Test
    fun testIdbFeatureValueAccess() {
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("CAN", "123456")
            .build()

        val feature = messageGroup.featureList[0]
        Assert.assertNotNull(feature)
        Assert.assertEquals("CAN", feature.name)
        Assert.assertEquals(0x09, feature.tag)
    }
}
