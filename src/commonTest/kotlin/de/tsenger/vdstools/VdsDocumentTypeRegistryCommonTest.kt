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
        assertEquals(setOf(0, 1, 2, 3), registry.getMetadataTags("ADMINISTRATIVE_DOCUMENTS_V8"))
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

    // -------------------------------------------------------------------------
    // ADMINISTRATIVE_DOCUMENTS_V9 — TR-03171 v0.9, document category 0xC9
    // -------------------------------------------------------------------------

    @Test
    fun testAdministrativeDocumentsV9_isRegistered() {
        val jsonString = readTextResource("VdsDocumentTypes.json")
        val registry = VdsDocumentTypeRegistry(jsonString)
        assertTrue(registry.availableVdsTypes.contains("ADMINISTRATIVE_DOCUMENTS_V9"))
    }

    @Test
    fun testAdministrativeDocumentsV9_documentRef() {
        // "01c9" hex → 0x01C9 = 457 dec
        // upper byte 0x01 = Document Feature Definition Reference, lower byte 0xC9 = category 201
        assertEquals(0x01C9, DataEncoder.vdsDocumentTypes.getDocumentRef("ADMINISTRATIVE_DOCUMENTS_V9"))
    }

    @Test
    fun testAdministrativeDocumentsV9_requiresProfileLookup() {
        assertTrue(DataEncoder.vdsDocumentTypes.requiresProfileLookup("ADMINISTRATIVE_DOCUMENTS_V9"))
    }

    @Test
    fun testAdministrativeDocumentsV9_uuidMessageTag() {
        assertEquals(0, DataEncoder.vdsDocumentTypes.getUuidMessageTag("ADMINISTRATIVE_DOCUMENTS_V9"))
    }

    @Test
    fun testAdministrativeDocumentsV9_metadataTagsContainAllReservedTags() {
        // Tags 0x00–0x06 are reserved for mandatory/optional metadata per TR-03171 v0.9 §3.2
        assertEquals(
            setOf(0, 1, 2, 3, 4, 5, 6),
            DataEncoder.vdsDocumentTypes.getMetadataTags("ADMINISTRATIVE_DOCUMENTS_V9")
        )
    }

    @Test
    fun testAdministrativeDocumentsV9_legacyTypeStillHasOldMetadataTags() {
        // Backward compat: 0xC8 still uses the old 4-tag set (tags 0–3 were RFU in v0.8)
        assertEquals(
            setOf(0, 1, 2, 3),
            DataEncoder.vdsDocumentTypes.getMetadataTags("ADMINISTRATIVE_DOCUMENTS_V8")
        )
    }
}