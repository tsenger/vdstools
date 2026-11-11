package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.FormatFinding.Category
import kotlin.test.*

/** Format findings of VDS seals (ICAO Doc 9303-13 App. D, format validation; TR-03171). */
@OptIn(ExperimentalStdlibApi::class)
class VdsFormatFindingsCommonTest {

    private val registeredUuid = "AABBCCDD11223344AABBCCDD11223344"
    private val unregisteredUuid = "11111111222222223333333344444444"

    /** 44-byte TR-03171 header without its last byte, the document type category (0xC9 = v0.9, 0xC8 = v0.8). */
    private val headerWithoutCategory =
        "dc036abc6d38dc0519a620372ce13372401c46ad535759e866926d2379bd19ce2678fe348d7ad88d7ad801"

    private val profileXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <profile>
            <profileNumber>$registeredUuid</profileNumber>
            <versionTR>0.9</versionTR>
            <profileName>TEST_V9_PROFILE</profileName>
            <creator>Test</creator>
            <validFromPresent>false</validFromPresent>
            <validToPresent>false</validToPresent>
            <entry tag="10">
                <name>SURNAME</name>
                <description>Familienname</description>
                <type>UTF8String</type>
            </entry>
            <entry tag="11" optional="true">
                <name>NOTE</name>
                <description>Bemerkung</description>
                <length>6</length>
                <type>UTF8String</type>
            </entry>
            <entry tag="13" optional="true">
                <name>BIRTH_DATE</name>
                <description>Geburtsdatum</description>
                <type>DATE</type>
            </entry>
            <entry tag="14" optional="true">
                <name>CODE</name>
                <description>Code</description>
                <length>2</length>
                <type>OCTET_STRING</type>
            </entry>
        </profile>
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml)
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun tlv(tag: Int, hex: String) = DerTlv(tag.toByte(), hex.hexToByteArray())
    private fun utf8(tag: Int, text: String) = DerTlv(tag.toByte(), text.encodeToByteArray())

    private val profileUri = utf8(3, "example.com/profiles")
    private val certificateUri = utf8(4, "example.com/certs")
    private val surname = utf8(0x0A, "Mustermann")

    private fun seal(category: String, vararg tlvs: DerTlv): VdsSeal {
        val messageZone = tlvs.fold(ByteArray(0)) { acc, tlv -> acc + tlv.encoded }
        return VdsSeal.fromByteArray((headerWithoutCategory + category).hexToByteArray() + messageZone) as VdsSeal
    }

    @Test
    fun conformingV9Seal_hasNoFindings() {
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun knownTypeFixtures_haveNoFindings() {
        val fixtures = mapOf(
            "residentPermit" to VdsRawBytesCommon.residentPermit,
            "addressStickerId" to VdsRawBytesCommon.addressStickerId,
            "socialInsurance" to VdsRawBytesCommon.socialInsurance,
            "arrivalAttestationV02" to VdsRawBytesCommon.arrivalAttestationV02,
            "arrivalAttestation" to VdsRawBytesCommon.arrivalAttestation,
            "supplementSheet" to VdsRawBytesCommon.supplementSheet,
            "addressStickerPassport" to VdsRawBytesCommon.addressStickerPassport,
            "emergenyTravelDoc" to VdsRawBytesCommon.emergenyTravelDoc,
            "ankunftsnwPapier" to VdsRawBytesCommon.ankunftsnwPapier,
            "ankunftsnwPapier2" to VdsRawBytesCommon.ankunftsnwPapier2,
        )
        for ((name, raw) in fixtures) {
            assertEquals(emptyList(), (VdsSeal.fromByteArray(raw) as VdsSeal).formatFindings, name)
        }
    }

    @Test
    fun v9WithoutProfile_reportsOnlyProfileNotFound() {
        // content tags are not checkable without profile — no UnknownTag per content tag
        val seal = seal("c9", tlv(0, unregisteredUuid), profileUri, certificateUri, tlv(0x0A, "4d75"), tlv(0x0C, "01"))

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.ProfileNotFound(unregisteredUuid, "example.com/profiles"), finding)
        assertEquals(Category.UNDETERMINED, finding.category)
        assertEquals(emptyList(), finding.tagPath)
    }

    @Test
    fun v9WithProfile_tagNotInProfileIsUnknownTag() {
        // tag 0x0C is in neither the base type nor TEST_V9_PROFILE (0x0A SURNAME, 0x0B NOTE)
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, tlv(0x0C, "abcd"))

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.UnknownTag(listOf(0x0C)), finding)
        assertEquals(Category.UNKNOWN_FEATURE, finding.category)
    }

    @Test
    fun undecodableProfileField_isDecodeFailedAndRaw() {
        // BIRTH_DATE (tag 0x0D) is a DATE: 8 UTF-8 digits YYYYMMDD; month 13 cannot be decoded
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, utf8(0x0D, "20251301"))

        val birthDate = seal.messageList.single { it.tag == 0x0D }
        assertEquals("BIRTH_DATE", birthDate.name)
        assertTrue(birthDate.isRaw)
        assertTrue(birthDate.isDefined, "a defined field that cannot be decoded is still defined")
        assertEquals("3230323531333031", birthDate.value.toString())

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.DecodeFailed(listOf(0x0D), "BIRTH_DATE"), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }

    @Test
    fun invalidUtf8ProfileField_isDecodeFailedAndRaw() {
        // C3 28 is an invalid UTF-8 sequence; it must not be replaced by U+FFFD silently
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, tlv(0x0A, "c328ff"))

        val surname = seal.messageList.single { it.tag == 0x0A }
        assertTrue(surname.isRaw)
        assertTrue(surname.isDefined)
        assertEquals(listOf(FormatFinding.DecodeFailed(listOf(0x0A), "SURNAME")), seal.formatFindings)
    }

    @Test
    fun invalidUtf8ProfileField_isNotLengthChecked() {
        // NOTE allows 6 characters; the undecodable bytes are reported as DecodeFailed only
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, tlv(0x0B, "ff".repeat(7)))
        assertEquals(listOf(FormatFinding.DecodeFailed(listOf(0x0B), "NOTE")), seal.formatFindings)
    }

    @Test
    fun customBaseTypeWithoutProfileNumberDefinition_reportsInsteadOfThrowing() {
        DataEncoder.addCustomVdsDocumentTypes(
            """
            [{
                "documentType": "CUSTOM_PROFILE_BASE",
                "documentRef": "fcfe",
                "version": 4,
                "uuidMessageLookup": true,
                "uuidMessageTag": 0,
                "metadataTagList": [0],
                "messages": [
                    {"name": "PAYLOAD", "tag": 10, "coding": "BYTES", "required": false, "minBytes": 1, "maxBytes": 10}
                ]
            }]
            """.trimIndent()
        )
        val group = VdsMessageGroup("CUSTOM_PROFILE_BASE", listOf(tlv(0x0A, "aa")))

        assertEquals(
            listOf(FormatFinding.ProfileNotFound(null, null), FormatFinding.MissingRequired(listOf(0), "0x00")),
            group.formatFindings()
        )
    }

    @Test
    fun decodableProfileField_isNotRaw() {
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, utf8(0x0D, "20251231"))

        val birthDate = seal.messageList.single { it.tag == 0x0D }
        assertFalse(birthDate.isRaw)
        assertTrue(birthDate.isDefined)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun utf8LengthIsCountedInCharacters() {
        // NOTE has <length>6</length>: "Müller" is 6 characters (7 bytes) and fits, "Müllers" does not
        val fits = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, utf8(0x0B, "Müller"))
        assertEquals(emptyList(), fits.formatFindings)

        val tooLong = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, utf8(0x0B, "Müllers"))
        val finding = tooLong.formatFindings.single()
        assertEquals(FormatFinding.InvalidLength(listOf(0x0B), "NOTE", 7, null, 6), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }

    @Test
    fun octetStringLengthIsCountedInBytes() {
        // CODE has <length>2</length>
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, surname, tlv(0x0E, "010203"))
        assertEquals(listOf(FormatFinding.InvalidLength(listOf(0x0E), "CODE", 3, null, 2)), seal.formatFindings)
    }

    @Test
    fun fieldWithoutExplicitLength_isNotLengthChecked() {
        // SURNAME has no <length>; the converter's default limit is not a profile requirement
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, utf8(0x0A, "x".repeat(300)))
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun v9ReservedMetadataTag_isReservedTag() {
        val seal = seal("c9", tlv(0, registeredUuid), profileUri, certificateUri, tlv(0x07, "01"), surname)

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.ReservedTag(listOf(0x07)), finding)
        assertEquals(Category.UNKNOWN_FEATURE, finding.category)
    }

    @Test
    fun v9MissingCertificateUriAndRequiredProfileField_areMissingRequired() {
        // NOTE (tag 11) is optional and absent → no finding
        val seal = seal("c9", tlv(0, registeredUuid), profileUri)

        assertEquals(
            listOf(
                FormatFinding.MissingRequired(listOf(4), "CERTIFICATE_URI"),
                FormatFinding.MissingRequired(listOf(0x0A), "SURNAME"),
            ),
            seal.formatFindings
        )
        assertTrue(seal.formatFindings.all { it.category == Category.WRONG_FORMAT })
    }

    @Test
    fun v9ValidFromAgainstProfile_isNotAllowed() {
        // TEST_V9_PROFILE declares validFromPresent=false
        val validFrom = utf8(1, "20250101")
        val seal = seal("c9", tlv(0, registeredUuid), validFrom, profileUri, certificateUri, surname)

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.NotAllowed(listOf(1), "VALID_FROM"), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }

    @Test
    fun v9WithoutProfileNumber_isMissingRequiredAndProfileNotFound() {
        val seal = seal("c9", profileUri, certificateUri, surname)

        assertEquals(
            listOf(
                FormatFinding.ProfileNotFound(null, "example.com/profiles"),
                FormatFinding.MissingRequired(listOf(0), "DOC_PROFILE_NUMBER"),
            ),
            seal.formatFindings
        )
    }

    @Test
    fun v8WithoutProfile_reservedMetadataTagIsReported() {
        // v0.8: tags 0x00–0x03 are metadata, 0x02/0x03 have no definition
        val seal = seal("c8", tlv(0, unregisteredUuid), tlv(0x02, "01"), tlv(0x04, "aa"))

        assertEquals(
            listOf(
                FormatFinding.ProfileNotFound(unregisteredUuid, null),
                FormatFinding.ReservedTag(listOf(0x02)),
            ),
            seal.formatFindings
        )
    }

    @Test
    fun unknownDocumentType_isUndetermined() {
        val seal = seal("99", tlv(0x0A, "aabb"))

        val finding = seal.formatFindings.single()
        assertEquals(FormatFinding.UnknownDocumentType(0x0199), finding)
        assertEquals(Category.UNDETERMINED, finding.category)
        assertEquals(emptyList(), finding.tagPath)
    }
}
