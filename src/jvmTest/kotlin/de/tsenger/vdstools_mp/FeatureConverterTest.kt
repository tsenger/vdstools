package de.tsenger.vdstools_mp


import de.tsenger.vdstools_mp.asn1.DerTlv
import okio.FileNotFoundException
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class FeatureConverterTest {
//    @Test
//    fun testFeatureConverter() {
//        FeatureConverter()
//    }

    @Test
    fun testFeatureConverterString() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        FeatureConverter(jsonString)
    }

    @Test(expected = FileNotFoundException::class)
    fun testFeatureConverterString_notFound() {
        val jsonString = FileLoader().loadFileFromResources("Codings.json")
        FeatureConverter(jsonString)
    }

    @Test
    fun testGetFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val feature = featureConverter.getFeatureName(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("PASSPORT_NUMBER", feature)
    }

    @Test
    fun testDecodeFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val value = featureConverter.decodeFeature<String>(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("UFO001979", value)
    }

    @Test
    fun testEncodeFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
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
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsTypes)
        Assert.assertTrue(featureConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsFeatures() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsFeatures)
        Assert.assertTrue(featureConverter.availableVdsFeatures.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        Assert.assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
