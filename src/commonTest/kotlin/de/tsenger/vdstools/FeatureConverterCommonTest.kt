package de.tsenger.vdstools


import de.tsenger.vdstools.asn1.DerTlv
import okio.FileNotFoundException
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class FeatureConverterCommonTest {
//    @Test
//    fun testFeatureConverter() {
//        FeatureConverter()
//    }

    @Test
    fun testFeatureConverterString() {
        val jsonString = readTextResource("SealCodings.json")
        FeatureConverter(jsonString)
    }

    @Test
    fun testFeatureConverterString_notFound() {
        assertFailsWith<FileNotFoundException> { readTextResource("Codings.json") }

    }


    @Test
    fun testGetFeature_String() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val feature = featureConverter.getFeatureName(
            "FICTION_CERT",
            DerTlv.fromByteArray(
                "0306d79519a65306".hexToByteArray()
            )!!
        )
        assertEquals("PASSPORT_NUMBER", feature)
    }


    @Test
    fun testEncodeFeature_String() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = featureConverter.encodeFeature("RESIDENCE_PERMIT", "MRZ", mrz)
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            derTlv.encoded.toHexString()
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsTypes)
        assertTrue(featureConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsFeatures() {
        val jsonString = readTextResource("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsFeatures)
        assertTrue(featureConverter.availableVdsFeatures.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
