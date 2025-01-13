package de.tsenger.vdstools_mp


import de.tsenger.vdstools_mp.asn1.DerTlv
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
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        FeatureConverter(jsonString)
    }

    @Test
    fun testFeatureConverterString_notFound() {
        assertFailsWith<FileNotFoundException> { FileLoader().loadFileFromResources("Codings.json") }

    }


    @Test
    fun testGetFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
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
    fun testDecodeFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        val value = featureConverter.decodeFeature<String>(
            "FICTION_CERT",
            DerTlv.fromByteArray("0306d79519a65306".hexToByteArray())!!
        )
        assertEquals("UFO001979", value)
    }

    @Test
    fun testEncodeFeature_String() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
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
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsTypes)
        assertTrue(featureConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsFeatures() {
        val jsonString = FileLoader().loadFileFromResources("SealCodings.json")
        val featureConverter = FeatureConverter(jsonString)
        println(featureConverter.availableVdsFeatures)
        assertTrue(featureConverter.availableVdsFeatures.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
