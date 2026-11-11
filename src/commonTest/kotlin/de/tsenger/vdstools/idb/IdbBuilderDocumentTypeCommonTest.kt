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
    fun customIdentifier_isUsed() {
        // own definitions: the identifier is another tag, 0x86 no longer marked
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "NATIONAL_DOCUMENT_IDENTIFIER", "tag": 134, "coding": "BYTE"},
                {"name": "MY_DOCUMENT_TYPE", "tag": 144, "coding": "BYTE", "identifiesDocumentType": true},
                {"name": "MY_NUMBER", "tag": 145, "coding": "C40"}]"""
        )
        DataEncoder.addCustomIdbDocumentTypes("""[{"tag": 1, "name": "MY_TYPE", "messages": [{"name": "MY_NUMBER", "required": true}]}]""")
        val seal = parse(IdbSeal.Builder("MY_TYPE").addMessage("MY_NUMBER", "AB123").build())
        assertEquals("MY_TYPE", seal.documentType)
        assertEquals(listOf(0x91, 0x90), seal.messageList.map { it.tag })
    }

    @Test
    fun noOrSeveralIdentifiers_areRejected() {
        DataEncoder.addCustomIdbMessageTypes("""[{"name": "NATIONAL_DOCUMENT_IDENTIFIER", "tag": 134, "coding": "BYTE"}]""")
        val none = assertFailsWith<IllegalArgumentException> { IdbSeal.Builder("CERTIFYING_PERMANENT_RESIDENCE") }
        assertTrue(none.message!!.contains("identifiesDocumentType"), none.message)

        DataEncoder.resetToDefaults()
        DataEncoder.addCustomIdbMessageTypes("""[{"name": "MY_DOCUMENT_TYPE", "tag": 144, "coding": "BYTE", "identifiesDocumentType": true}]""")
        val several = assertFailsWith<IllegalArgumentException> { IdbSeal.Builder("CERTIFYING_PERMANENT_RESIDENCE") }
        assertTrue(several.message!!.contains("MY_DOCUMENT_TYPE"), several.message)
    }
}
