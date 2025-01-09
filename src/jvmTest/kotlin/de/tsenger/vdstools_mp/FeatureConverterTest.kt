package de.tsenger.vdstools_mp


import de.tsenger.vdstools_mp.asn1.DerTlv
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class FeatureConverterTest {
    @Test
    fun testFeatureConverter() {
        FeatureConverter()
    }

    @Test
    fun testFeatureConverterString() {
        val fe = File("src/jvmTest/resources/SealCodings.json")
        val fis = FileInputStream(fe)
        FeatureConverter(fis)
    }

    @Test(expected = FileNotFoundException::class)
    fun testFeatureConverterString_notFound() {
        val fe = File("src/jvmTest/resources/Codings.json")
        val fis = FileInputStream(fe)
        FeatureConverter(fis)
    }

    @Test
    fun testGetFeature_String() {
        val featureConverter = FeatureConverter()
        val feature = featureConverter.getFeatureName(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("PASSPORT_NUMBER", feature)
    }

    @Test
    fun testDecodeFeature_String() {
        val featureConverter = FeatureConverter()
        val value = featureConverter.decodeFeature<String>(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("UFO001979", value)
    }

    @Test
    fun testEncodeFeature_String() {
        val featureConverter = FeatureConverter()
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = featureConverter.encodeFeature("RESIDENCE_PERMIT", "MRZ", mrz)
        Assert.assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            Hex.toHexString(derTlv.encoded)
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val featureConverter = FeatureConverter()
        println(featureConverter.availableVdsTypes)
        Assert.assertTrue(featureConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsFeatures() {
        val featureConverter = FeatureConverter()
        println(featureConverter.availableVdsFeatures)
        Assert.assertTrue(featureConverter.availableVdsFeatures.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        Assert.assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
