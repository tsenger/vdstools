package de.tsenger.vdstools


import de.tsenger.vdstools.asn1.DerTlv
import okio.FileNotFoundException
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class VdsDocumentTypeRegistryCommonTest {

    @Test
    fun testVdsDocumentTypeRegistryString() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        VdsDocumentTypeRegistry(jsonString)
    }

    @Test
    fun testVdsDocumentTypeRegistryString_notFound() {
        assertFailsWith<FileNotFoundException> { readTextResource("Codings.json") }

    }


    @Test
    fun testGetMessage_String() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        val message = registry.getMessageName(
            "FICTION_CERT",
            DerTlv.fromByteArray(
                "0306d79519a65306".hexToByteArray()
            )!!
        )
        assertEquals("PASSPORT_NUMBER", message)
    }


    @Test
    fun testEncodeMessage_String() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = registry.encodeMessage("RESIDENCE_PERMIT", "MRZ", mrz)
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            derTlv.encoded.toHexString()
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        println(registry.availableVdsTypes)
        assertTrue(registry.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsMessages() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        println(registry.availableVdsMessages)
        assertTrue(registry.availableVdsMessages.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        assertNull(DataEncoder.vdsDocumentTypes.getDocumentRef("FAKE_SEAL"))
    }

    @Test
    fun testGetMetadataTags_administrativeDocuments() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        assertEquals(setOf(0, 1, 2, 3), registry.getMetadataTags("ADMINISTRATIVE_DOCUMENTS"))
    }

    @Test
    fun testGetMetadataTags_unknownType_returnsEmpty() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        assertTrue(registry.getMetadataTags("UNKNOWN_TYPE").isEmpty())
    }

    @Test
    fun testGetMetadataTags_regularType_returnsEmpty() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        assertTrue(registry.getMetadataTags("RESIDENT_PERMIT").isEmpty())
    }
}