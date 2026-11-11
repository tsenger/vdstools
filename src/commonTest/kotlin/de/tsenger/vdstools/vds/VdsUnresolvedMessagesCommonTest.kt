package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.dissect.dissect
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import kotlin.test.*

/**
 * Tags that neither the base type nor a resolved profile defines are returned raw
 * (ICAO Doc 9303-13 §2.3: unknown tags MAY be present): name "0x" + two hex digits,
 * coding [MessageCoding.UNKNOWN], [MessageValue.BytesValue] with the original bytes.
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsUnresolvedMessagesCommonTest {

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

    private fun seal(category: String, vararg tlvs: DerTlv): VdsSeal {
        val messageZone = tlvs.fold(ByteArray(0)) { acc, tlv -> acc + tlv.encoded }
        return VdsSeal.fromByteArray((headerWithoutCategory + category).hexToByteArray() + messageZone) as VdsSeal
    }

    private fun assertRaw(message: Message, tag: Int, hex: String) {
        assertEquals(tag, message.tag)
        assertEquals("0x" + tag.toString(16).uppercase().padStart(2, '0'), message.name)
        assertEquals(MessageCoding.UNKNOWN, message.coding)
        assertTrue(message.isRaw)
        assertFalse(message.isDefined)
        assertTrue(message.value is MessageValue.BytesValue)
        assertEquals(hex, message.value.rawBytes.toHexString())
    }

    @Test
    fun v9_withoutProfile_contentTagsAreRawInSealOrder() {
        val seal = seal("c9", tlv(0, unregisteredUuid), tlv(0x0C, "01"), tlv(0x0A, "4d75"))

        assertNull(seal.baseDocumentType, "no profile must be resolved")
        assertEquals(listOf("0x0C", "0x0A"), seal.messageList.map { it.name })
        assertRaw(seal.messageList[0], 0x0C, "01")
        assertRaw(seal.messageList[1], 0x0A, "4d75")
    }

    @Test
    fun v9_withoutProfile_metadataTagsKeepTheirNames() {
        val profileUri = "example.com/profiles".encodeToByteArray().toHexString()
        val seal = seal("c9", tlv(0, unregisteredUuid), tlv(3, profileUri), tlv(0x0A, "4d75"))

        assertEquals(listOf("0x0A"), seal.messageList.map { it.name })
        assertEquals(listOf("DOC_PROFILE_NUMBER", "PROFILE_URI"), seal.metadataMessageList.map { it.name })
        assertTrue(seal.metadataMessageList.none { it.isRaw })
        assertTrue(seal.metadataMessageList.all { it.isDefined })
    }

    @Test
    fun v9_withProfile_tagNotInProfileIsRaw() {
        val surname = "Mustermann".encodeToByteArray().toHexString()
        val seal = seal("c9", tlv(0, registeredUuid), tlv(0x0A, surname), tlv(0x0B, "abcd"))

        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", seal.baseDocumentType)
        assertEquals(listOf("SURNAME", "0x0B"), seal.messageList.map { it.name })
        assertEquals("Mustermann", seal.messageList[0].value.toString())
        assertFalse(seal.messageList[0].isRaw)
        assertTrue(seal.messageList[0].isDefined)
        assertRaw(seal.messageList[1], 0x0B, "abcd")
    }

    @Test
    fun v9_reservedMetadataTagIsRawInMetadataList() {
        val surname = "Mustermann".encodeToByteArray().toHexString()
        val seal = seal("c9", tlv(0, registeredUuid), tlv(0x07, "01"), tlv(0x0A, surname))

        assertEquals(listOf("SURNAME"), seal.messageList.map { it.name })
        val reserved = seal.metadataMessageList.single { it.tag == 0x07 }
        assertRaw(reserved, 0x07, "01")
    }

    @Test
    fun v8_withoutProfile_contentTagsAreRawInSealOrder() {
        val seal = seal("c8", tlv(0, unregisteredUuid), tlv(0x04, "aa"), tlv(0x05, "bbcc"))

        assertEquals(listOf("0x04", "0x05"), seal.messageList.map { it.name })
        assertRaw(seal.messageList[0], 0x04, "aa")
        assertRaw(seal.messageList[1], 0x05, "bbcc")
    }

    @Test
    fun icaoType_unknownTagIsRaw() {
        val group = VdsMessageGroup("RESIDENCE_PERMIT", listOf(tlv(0x30, "aabb")))

        assertEquals(1, group.messageList.size)
        assertRaw(group.messageList[0], 0x30, "aabb")
    }

    @Test
    fun unknownDocumentType_tagsAreRaw() {
        val group = VdsMessageGroup(VdsHeader.UNKNOWN_TYPE, listOf(tlv(0x0A, "aabb")))

        assertRaw(group.messageList.single(), 0x0A, "aabb")
    }

    @Test
    fun v9_withoutProfile_dissectionLabelsContentTagsByHex() {
        val seal = seal("c9", tlv(0, unregisteredUuid), tlv(0x0A, "4d75"))

        val labels = seal.dissect().messageGroup.children.map { it.label }
        assertEquals(listOf("DOC_PROFILE_NUMBER", "0x0A"), labels)
    }
}
