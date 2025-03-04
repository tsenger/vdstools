package de.tsenger.vdstools


import de.tsenger.vdstools.asn1.DerTlv
import okio.FileNotFoundException
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class FeatureConverterJvmTest {

    @Test
    fun testFeatureConverterString() {
        val jsonString = readTextResource("SealCodings.json")
        FeatureConverter(jsonString)
    }

    @Test(expected = FileNotFoundException::class)
    fun testFeatureConverterString_notFound() {
        val jsonString = readTextResource("Codings.json")
        FeatureConverter(jsonString)
    }

    @Test
    fun testGetFeature_String() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val feature = featureConverter.getFeatureName(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("PASSPORT_NUMBER", feature)
    }


    @Test
    fun testEncodeFeature_String() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = featureConverter.encodeFeature("RESIDENCE_PERMIT", "MRZ", mrz)
        Assert.assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            Hex.toHexString(derTlv.encoded)
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsTypes)
        Assert.assertTrue(featureConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsFeatures() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsFeatures)
        Assert.assertTrue(featureConverter.availableVdsFeatures.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        Assert.assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
