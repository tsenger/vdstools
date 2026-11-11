package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * National IDB message types (BSI TR-03137 §11.2.2: 0x80–0x86) and national document types belong to
 * their country. Lookup: the seal's country first, then international (ICAO) types, then a unique
 * national type of another country, reported per message as [FormatFinding.ForeignNationalDefinition].
 */
class IdbCountryLookupCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun c40(tag: Int, text: String) = DerTlv(tag.toByte(), DataEncoder.encodeC40(text))

    /** Builds and parses a seal, so the country takes the same path as for a real barcode. */
    private fun seal(country: String, block: IdbSeal.Builder.() -> Unit): IdbSeal {
        val built = IdbSeal.Builder().countryIdentifier(country).apply(block).build()
        return SealParser().parse(built.rawString) as IdbSeal
    }

    private fun foreign(seal: IdbSeal) = seal.formatFindings.filterIsInstance<FormatFinding.ForeignNationalDefinition>()

    @Test
    fun germanSeal_hasNoForeignFinding() {
        val seal = seal("D<<") { addMessage("DOCUMENT_NUMBER", "ZZ1234567") }
        assertEquals("DOCUMENT_NUMBER", seal.messageList.single().name)
        assertEquals(emptyList(), foreign(seal))
    }

    @Test
    fun specimenSeal_nationalMessageHasFinding() {
        val seal = seal("UTO") {
            addMessage("CAN", "123456")
            addMessage("DOCUMENT_NUMBER", "ZZ1234567")
        }
        assertEquals(listOf("CAN", "DOCUMENT_NUMBER"), seal.messageList.map { it.name })
        assertEquals(listOf(FormatFinding.ForeignNationalDefinition(listOf(0x85), "D")), foreign(seal))
    }

    @Test
    fun specimenSeal_isCheckedAgainstTheGermanDocumentType() {
        // Certifying Permanent Residence (0x10): 0x82, 0x85, 0x86 required
        val seal = seal("UTO") {
            addMessage("DOCUMENT_REFERENCE", "ABC123456")
            addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x10)
        }
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", seal.documentType)
        assertEquals(
            listOf(
                FormatFinding.ForeignNationalDefinition(listOf(0x82), "D"),
                FormatFinding.ForeignNationalDefinition(listOf(0x86), "D"),
                FormatFinding.MissingRequired(listOf(0x85), "DOCUMENT_NUMBER"),
            ),
            seal.formatFindings
        )
    }

    private fun addFrenchPhoto() = DataEncoder.addCustomIdbMessageTypes(
        """[{"name": "FR_PHOTO", "tag": 128, "coding": "BYTES", "country": "F"}]"""
    )

    @Test
    fun ownCountryIsPreferred() {
        addFrenchPhoto()
        val photo = byteArrayOf(1, 2, 3)

        assertEquals("FR_PHOTO", seal("F<<") { addMessage("FR_PHOTO", photo) }.messageList.single().name)
        assertEquals("FACE_IMAGE", seal("D<<") { addMessage("FACE_IMAGE", photo) }.messageList.single().name)
    }

    @Test
    fun ambiguousForeignTypes_areRaw() {
        addFrenchPhoto()
        val seal = seal("UTO") { addMessage("FACE_IMAGE", byteArrayOf(1, 2, 3)) }

        assertEquals("0x80", seal.messageList.single().name)
        assertEquals(listOf(FormatFinding.UnknownTag(listOf(0x80))), seal.formatFindings)
    }

    private fun addFrenchCode() = DataEncoder.addCustomIdbMessageTypes(
        """[{"name": "FR_CODE", "tag": 128, "coding": "C40", "country": "F"}]"""
    )

    @Test
    fun builderByTag_usesTheDefinitionOfTheIssuingCountry() {
        // 0x80 is FACE_IMAGE (BYTES) for D and FR_CODE (C40) for F; the country may be set after the message
        addFrenchCode()
        val french = SealParser().parse(
            IdbSeal.Builder().addMessage(0x80, "AB12").countryIdentifier("F<<").build().rawString
        ) as IdbSeal
        assertEquals("FR_CODE", french.messageList.single().name)
        assertEquals(emptyList(), french.formatFindings)

        val photo = byteArrayOf(1, 2, 3)
        val german = seal("D<<") { addMessage(0x80, photo) }
        assertEquals("FACE_IMAGE", german.messageList.single().name)
        assertContentEquals(photo, german.messageList.single().value.rawBytes)
    }

    @Test
    fun builderByTag_containerUsesTheDefinitionOfTheIssuingCountry() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "FR_CONTAINER", "tag": 129, "coding": "SUB_MESSAGES", "country": "F",
                 "messages": [{"name": "FR_ITEM", "tag": 1, "coding": "C40"}]}]"""
        )
        val seal = seal("F<<") { addMessage(0x81) { addMessage("FR_ITEM", "XY") } }
        assertEquals("FR_ITEM", seal.messageList.single().messageList.single().name)
    }

    @Test
    fun icaoMessages_haveNoFinding() {
        val seal = seal("UTO") { addMessage("CAN", "123456") }
        assertEquals(emptyList(), seal.formatFindings)
    }
}
