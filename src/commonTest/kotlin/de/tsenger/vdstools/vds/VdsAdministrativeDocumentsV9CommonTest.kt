package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.MessageValue
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests for the ADMINISTRATIVE_DOCUMENTS_V9 document type (TR-03171 v0.9,
 * document category 0xC9). Covers header encoding, message-zone structure, metadata/content
 * separation, and the new DATE_STRING validity-date fields.
 *
 * These tests run on all platforms and do not require a keystore or signature verification.
 * Full signed-seal creation is covered by the JVM-only [CreateTR03171Seals] tests.
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsAdministrativeDocumentsV9CommonTest {

    // UUID used by all tests; kept short enough to identify but valid as a 16-byte hex string
    private val testUuid = "AABBCCDD11223344AABBCCDD11223344"
    private val testUuidHex = testUuid.lowercase() // "aabbccdd11223344aabbccdd11223344"

    /**
     * Minimal profile with one content field (tag 0x0A = 10).
     * Content tags start at 0x0A per TR-03171 v0.9 §3.2.
     */
    private val testProfileXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <profile>
            <profileNumber>$testUuid</profileNumber>
            <profileName>TEST_V9_PROFILE</profileName>
            <creator>Test</creator>
            <entry tag="10">
                <name>SURNAME</name>
                <description>Familienname</description>
                <type>UTF8String</type>
            </entry>
        </profile>
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
        // Default base type is ADMINISTRATIVE_DOCUMENTS_V9 — no explicit parameter needed
        DataEncoder.loadVdsProfileDefinitionFromXml(testProfileXml)
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    @Test
    fun testV9Header_documentTypeCategoryByteIsC9() {
        // Building via profile name must resolve to the 0xC9 base type
        val header = VdsHeader.Builder("TEST_V9_PROFILE")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        // Last two bytes of the 44-byte header: Document Feature Definition Reference (0x01)
        // followed by Document Type Category (0xC9)
        val encoded = header.encoded
        assertEquals(0x01.toByte(), encoded[encoded.size - 2], "Expected docFeatureRef = 0x01")
        assertEquals(0xC9.toByte(), encoded[encoded.size - 1], "Expected docTypeCat = 0xC9")
    }

    @Test
    fun testV9Header_vdsTypeResolvesToAdministrativeDocumentsV9() {
        val header = VdsHeader.Builder("TEST_V9_PROFILE")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", header.vdsType)
    }

    @Test
    fun testV9Header_encodedBytesEndWithC9() {
        // Same params as the legacy 0xC8 test — only the last byte differs
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS_V9")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        assertEquals(
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c9",
            header.encoded.toHexString()
        )
    }

    // -------------------------------------------------------------------------
    // Message Zone — UUID injection (Tag 0x00)
    // -------------------------------------------------------------------------

    @Test
    fun testV9MessageGroup_uuidIsInjectedAsFirstTlv() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("SURNAME", "Mustermann")
            .build()

        // Tag 0 must be first in the wire encoding
        val encoded = group.encoded
        assertEquals(0x00.toByte(), encoded[0], "First tag in message zone must be 0x00 (UUID)")
        assertEquals(0x10.toByte(), encoded[1], "UUID length must be 16 bytes (0x10)")
    }

    @Test
    fun testV9MessageGroup_documentProfileUuidIsSet() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("SURNAME", "Mustermann")
            .build()

        assertNotNull(group.documentProfileUuid)
        assertEquals(16, group.documentProfileUuid!!.size)
        assertEquals(testUuidHex, group.documentProfileUuid!!.toHexString())
    }

    // -------------------------------------------------------------------------
    // Message Zone — mandatory metadata (Tags 0x03 and 0x04)
    // -------------------------------------------------------------------------

    @Test
    fun testV9MessageGroup_profileUriInMetadataList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("SURNAME", "Mustermann")
            .build()

        val profileUri = group.metadataMessageList.firstOrNull { it.name == "PROFILE_URI" }
        assertNotNull(profileUri, "PROFILE_URI must be present in metadataMessageList")
        assertEquals("example.com/profiles", profileUri.value.toString())
    }

    @Test
    fun testV9MessageGroup_certificateUriInMetadataList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("SURNAME", "Mustermann")
            .build()

        val certUri = group.metadataMessageList.firstOrNull { it.name == "CERTIFICATE_URI" }
        assertNotNull(certUri, "CERTIFICATE_URI must be present in metadataMessageList")
        assertEquals("example.com/certs", certUri.value.toString())
    }

    @Test
    fun testV9MessageGroup_profileAndCertUriNotInMessageList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("SURNAME", "Mustermann")
            .build()

        assertNull(group.messageList.firstOrNull { it.name == "PROFILE_URI" },
            "PROFILE_URI is metadata and must not appear in messageList")
        assertNull(group.messageList.firstOrNull { it.name == "CERTIFICATE_URI" },
            "CERTIFICATE_URI is metadata and must not appear in messageList")
    }

    // -------------------------------------------------------------------------
    // Message Zone — optional metadata (Tags 0x05 and 0x06)
    // -------------------------------------------------------------------------

    @Test
    fun testV9MessageGroup_statusUriInMetadataList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("STATUS_URI", "example.com/status")
            .addMessage("SURNAME", "Mustermann")
            .build()

        val statusUri = group.metadataMessageList.firstOrNull { it.name == "STATUS_URI" }
        assertNotNull(statusUri, "STATUS_URI must be present in metadataMessageList")
        assertEquals("example.com/status", statusUri.value.toString())
    }

    @Test
    fun testV9MessageGroup_statusListIndexInMetadataList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("STATUS_LIST_INDEX", byteArrayOf(0x06, 0x79, 0x32)) // 424242 decimal
            .addMessage("SURNAME", "Mustermann")
            .build()

        val idx = group.metadataMessageList.firstOrNull { it.name == "STATUS_LIST_INDEX" }
        assertNotNull(idx, "STATUS_LIST_INDEX must be present in metadataMessageList")
        assertEquals("067932", idx.value.toString())
    }

    // -------------------------------------------------------------------------
    // Message Zone — validity dates (Tags 0x01 and 0x02, DATE_STRING encoding)
    // -------------------------------------------------------------------------

    @Test
    fun testV9ValidFrom_encodedAsDateString() {
        val date = LocalDate(2025, 1, 1)
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("VALID_FROM", date)
            .addMessage("SURNAME", "Mustermann")
            .build()

        val validFrom = group.metadataMessageList.firstOrNull { it.name == "VALID_FROM" }
        assertNotNull(validFrom, "VALID_FROM must be present in metadataMessageList")

        // Must decode to DateValue (from DATE_STRING coding)
        assertTrue(validFrom.value is MessageValue.DateValue,
            "VALID_FROM must decode to DateValue, got ${validFrom.value::class.simpleName}")
        assertEquals(date, (validFrom.value as MessageValue.DateValue).date)

        // Raw bytes must be the 8-byte YYYYMMDD UTF-8 string, not the 3-byte ICAO format
        assertEquals(8, validFrom.value.rawBytes.size,
            "DATE_STRING must be 8 bytes, not the 3-byte ICAO format")
        assertEquals("20250101", validFrom.value.rawBytes.decodeToString())
    }

    @Test
    fun testV9ValidTo_encodedAsDateString() {
        val date = LocalDate(2025, 12, 31)
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("VALID_TO", date)
            .addMessage("SURNAME", "Mustermann")
            .build()

        val validTo = group.metadataMessageList.firstOrNull { it.name == "VALID_TO" }
        assertNotNull(validTo, "VALID_TO must be present in metadataMessageList")

        assertTrue(validTo.value is MessageValue.DateValue)
        assertEquals(date, (validTo.value as MessageValue.DateValue).date)
        assertEquals(8, validTo.value.rawBytes.size)
        assertEquals("20251231", validTo.value.rawBytes.decodeToString())
    }

    @Test
    fun testV9ValidFromAndValidTo_areMetadata_notInMessageList() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("VALID_FROM", LocalDate(2025, 1, 1))
            .addMessage("VALID_TO", LocalDate(2025, 12, 31))
            .addMessage("SURNAME", "Mustermann")
            .build()

        assertNull(group.messageList.firstOrNull { it.name == "VALID_FROM" })
        assertNull(group.messageList.firstOrNull { it.name == "VALID_TO" })
        assertNotNull(group.metadataMessageList.firstOrNull { it.name == "VALID_FROM" })
        assertNotNull(group.metadataMessageList.firstOrNull { it.name == "VALID_TO" })
    }

    // -------------------------------------------------------------------------
    // Message Zone — content separation
    // -------------------------------------------------------------------------

    @Test
    fun testV9MessageList_containsOnlyContentFields() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("VALID_FROM", LocalDate(2025, 1, 1))
            .addMessage("SURNAME", "Mustermann")
            .build()

        assertEquals(1, group.messageList.size,
            "Only content fields (tags ≥ 0x0A) must appear in messageList")
        assertEquals("SURNAME", group.messageList[0].name)
        assertEquals("Mustermann", group.messageList[0].value.toString())
    }

    @Test
    fun testV9ContentTag_startingAt0x0A() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("SURNAME", "Mustermann")
            .build()

        val surnameMsg = group.messageList.firstOrNull { it.name == "SURNAME" }
        assertNotNull(surnameMsg)
        assertEquals(0x0A, surnameMsg.tag,
            "Content fields in TR-03171 v0.9 start at tag 0x0A")
    }

    // -------------------------------------------------------------------------
    // Encoded bytes — spot-check TLV structure
    // -------------------------------------------------------------------------

    @Test
    fun testV9MessageGroup_encodedBytesContainUuidAndProfileUri() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("SURNAME", "Mustermann")
            .build()

        val expected =
            // Tag 0x00, length 0x10, UUID
            "0010aabbccdd11223344aabbccdd11223344" +
            // Tag 0x03, length 0x14 (20), "example.com/profiles"
            "03146578616d706c652e636f6d2f70726f66696c6573" +
            // Tag 0x04, length 0x11 (17), "example.com/certs"
            "04116578616d706c652e636f6d2f6365727473" +
            // Tag 0x0A, length 0x0A (10), "Mustermann"
            "0a0a4d75737465726d616e6e"

        assertEquals(expected, group.encoded.toHexString())
    }

    // -------------------------------------------------------------------------
    // Metadata tag set
    // -------------------------------------------------------------------------

    @Test
    fun testV9MetadataTagSet_containsAll7Tags() {
        val group = VdsMessageGroup.Builder("TEST_V9_PROFILE")
            .addMessage("SURNAME", "Mustermann")
            .build()

        // Tags 0x00–0x06 are reserved as metadata in TR-03171 v0.9 §3.2
        assertEquals(setOf(0, 1, 2, 3, 4, 5, 6), group.metadataTags)
    }

    // -------------------------------------------------------------------------
    // Backward compatibility — legacy 0xC8 type is unaffected
    // -------------------------------------------------------------------------

    @Test
    fun testLegacyC8Type_documentTypeCategoryIsC8() {
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS_V8")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        val encoded = header.encoded
        assertEquals(0xC8.toByte(), encoded[encoded.size - 1],
            "Legacy ADMINISTRATIVE_DOCUMENTS_V8 must still use 0xC8 category byte")
    }

    // -------------------------------------------------------------------------
    // Parsing — VdsSeal.fromByteArray with 0xC9 header bytes
    //
    // These tests verify the full decoding path:
    //   raw bytes → VdsHeader (docTypeCat=0xC9 → vdsType=ADMINISTRATIVE_DOCUMENTS_V9)
    //             → UUID lookup → profile resolution → metadata/content separation
    //
    // The test profile must be registered in setUp() before these tests run.
    // -------------------------------------------------------------------------

    @Test
    fun testParse_basic_documentTypeIsTestV9Profile() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        assertEquals("TEST_V9_PROFILE", seal.documentType)
    }

    @Test
    fun testParse_basic_baseDocumentTypeIsV9() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", seal.baseDocumentType)
    }

    @Test
    fun testParse_basic_profileUriInMetadataList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        val profileUri = seal.metadataMessageList.firstOrNull { it.name == "PROFILE_URI" }
        assertNotNull(profileUri)
        assertEquals("example.com/profiles", profileUri.value.toString())
    }

    @Test
    fun testParse_basic_certificateUriInMetadataList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        val certUri = seal.metadataMessageList.firstOrNull { it.name == "CERTIFICATE_URI" }
        assertNotNull(certUri)
        assertEquals("example.com/certs", certUri.value.toString())
    }

    @Test
    fun testParse_basic_contentFieldReachableByName() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
    }

    @Test
    fun testParse_basic_metadataNotInMessageList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        assertNull(seal.getMessageByName("PROFILE_URI"),
            "PROFILE_URI is metadata — must not appear in messageList")
        assertNull(seal.getMessageByName("CERTIFICATE_URI"),
            "CERTIFICATE_URI is metadata — must not appear in messageList")
        assertEquals(1, seal.messageList.size)
    }

    @Test
    fun testParse_basic_documentProfileUuidIsCorrect() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9Basic) as VdsSeal
        assertNotNull(seal.documentProfileUuid)
        assertEquals(testUuidHex, seal.documentProfileUuid!!.toHexString())
    }

    // --- Seal with validity dates ---

    @Test
    fun testParse_withDates_validFromDecodesAsDateValue() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9WithDates) as VdsSeal

        val validFrom = seal.metadataMessageList.firstOrNull { it.name == "VALID_FROM" }
        assertNotNull(validFrom, "VALID_FROM must be present in metadataMessageList")
        assertTrue(validFrom.value is MessageValue.DateValue,
            "VALID_FROM must decode to DateValue")
        assertEquals(LocalDate(2025, 1, 1), (validFrom.value as MessageValue.DateValue).date)
        // Raw bytes must be the 8-byte YYYYMMDD string (DATE_STRING coding)
        assertEquals(8, validFrom.value.rawBytes.size)
        assertEquals("20250101", validFrom.value.rawBytes.decodeToString())
    }

    @Test
    fun testParse_withDates_validToDecodesAsDateValue() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9WithDates) as VdsSeal

        val validTo = seal.metadataMessageList.firstOrNull { it.name == "VALID_TO" }
        assertNotNull(validTo, "VALID_TO must be present in metadataMessageList")
        assertEquals(LocalDate(2025, 12, 31), (validTo.value as MessageValue.DateValue).date)
        assertEquals("20251231", validTo.value.rawBytes.decodeToString())
    }

    @Test
    fun testParse_withDates_contentStillReachable() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9WithDates) as VdsSeal
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
    }

    // --- Seal with status fields ---

    @Test
    fun testParse_withStatus_statusUriInMetadataList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9WithStatus) as VdsSeal

        val statusUri = seal.metadataMessageList.firstOrNull { it.name == "STATUS_URI" }
        assertNotNull(statusUri, "STATUS_URI must be present in metadataMessageList")
        assertEquals("example.com/status", statusUri.value.toString())
    }

    @Test
    fun testParse_withStatus_statusListIndexInMetadataList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.administrativeDocumentV9WithStatus) as VdsSeal

        val statusIdx = seal.metadataMessageList.firstOrNull { it.name == "STATUS_LIST_INDEX" }
        assertNotNull(statusIdx, "STATUS_LIST_INDEX must be present in metadataMessageList")
        // Raw bytes: 0x067932 = 424242 decimal
        assertEquals("067932", statusIdx.value.toString())
    }

    // --- Header parsing ---

    @Test
    fun testParse_basic_headerVdsTypeIsV9() {
        val buffer = okio.Buffer().write(VdsRawBytesCommon.administrativeDocumentV9Basic)
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", header.vdsType)
    }

    @Test
    fun testParse_basic_headerDocTypeCatIsC9() {
        val buffer = okio.Buffer().write(VdsRawBytesCommon.administrativeDocumentV9Basic)
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals(0xC9.toByte(), header.docTypeCat)
    }
}
