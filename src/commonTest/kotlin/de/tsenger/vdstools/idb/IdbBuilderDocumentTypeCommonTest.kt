package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * `IdbSeal.Builder(documentType)` builds a seal of a national document type like `VdsSeal.Builder(name)`:
 * the message that identifies the document type (BSI TR-03137: 0x86) is set from the definition.
 */
class IdbBuilderDocumentTypeCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun parse(seal: IdbSeal) = SealParser().parse(seal.rawString) as IdbSeal

    @Test
    fun documentType_setsTheIdentifier() {
        val seal = parse(
            IdbSeal.Builder("CERTIFYING_PERMANENT_RESIDENCE").countryIdentifier("D<<")
                .addMessage("DOCUMENT_REFERENCE", "ABC123456")
                .addMessage("DOCUMENT_NUMBER", "ZZ1234567")
                .build()
        )
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", seal.documentType)
        assertEquals(listOf(0x82, 0x85, 0x86), seal.messageList.map { it.tag })
        assertEquals(0x10, (seal.messageList.last().value as MessageValue.ByteValue).value)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun unknownDocumentType_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> { IdbSeal.Builder("NO_SUCH_TYPE") }
        assertTrue(e.message!!.contains("NO_SUCH_TYPE"), e.message)
    }

    @Test
    fun identifierAddedByHand_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> {
            IdbSeal.Builder("CERTIFYING_PERMANENT_RESIDENCE")
                .addMessage("DOCUMENT_REFERENCE", "ABC123456")
                .addMessage("DOCUMENT_NUMBER", "ZZ1234567")
                .addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x10)
                .build()
        }
        assertTrue(e.message!!.contains("NATIONAL_DOCUMENT_IDENTIFIER"), e.message)
    }

    @Test
    fun anotherCountry_usesItsOwnIdentifier() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "FR_DOCUMENT_TYPE", "tag": 144, "coding": "BYTE", "country": "F", "identifiesDocumentType": true},
                {"name": "FR_NUMBER", "tag": 145, "coding": "C40", "country": "F"}]"""
        )
        DataEncoder.addCustomIdbDocumentTypes(
            """[{"tag": 1, "name": "FR_TITRE", "country": "F", "messages": [{"name": "FR_NUMBER", "required": true}]}]"""
        )
        val seal = parse(IdbSeal.Builder("FR_TITRE").countryIdentifier("F<<").addMessage("FR_NUMBER", "AB123").build())
        assertEquals("FR_TITRE", seal.documentType)
        assertEquals(listOf(0x91, 0x90), seal.messageList.map { it.tag })
    }

    @Test
    fun typeOfACountryWithoutIdentifier_isRejected() {
        DataEncoder.addCustomIdbDocumentTypes("""[{"tag": 1, "name": "XX_TYPE", "country": "XX"}]""")
        val e = assertFailsWith<IllegalArgumentException> { IdbSeal.Builder("XX_TYPE").build() }
        assertTrue(e.message!!.contains("XX_TYPE"), e.message)
    }
}
