package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * The message type that carries the national document type is marked in the data
 * (`identifiesDocumentType`, BSI TR-03137: 0x86 NATIONAL_DOCUMENT_IDENTIFIER), not in the code.
 */
class IdbDocumentTypeIdentifierCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun seal(country: String, block: IdbSeal.Builder.() -> Unit): IdbSeal {
        val built = IdbSeal.Builder().countryIdentifier(country).apply(block).build()
        return SealParser().parse(built.rawString) as IdbSeal
    }

    @Test
    fun bundledGermanIdentifierIsMarked() {
        assertTrue(DataEncoder.idbMessageTypes.getMessageTypeDto("NATIONAL_DOCUMENT_IDENTIFIER")!!.identifiesDocumentType)
        assertFalse(DataEncoder.idbMessageTypes.getMessageTypeDto("DOCUMENT_NUMBER")!!.identifiesDocumentType)
    }

    @Test
    fun customIdentifierTag_identifiesTheDocumentType() {
        // own definitions: another tag carries the document type, checked against its rules
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "NATIONAL_DOCUMENT_IDENTIFIER", "tag": 134, "coding": "BYTE"},
                {"name": "MY_DOCUMENT_TYPE", "tag": 144, "coding": "BYTE", "identifiesDocumentType": true},
                {"name": "MY_NUMBER", "tag": 145, "coding": "C40"}]"""
        )
        DataEncoder.addCustomIdbDocumentTypes(
            """[{"tag": 80, "name": "MY_TYPE", "specification": "Test spec",
                 "messages": [{"name": "MY_DOCUMENT_TYPE", "required": true}, {"name": "MY_NUMBER", "required": true}]}]"""
        )
        val seal = seal("D<<") { addMessage("MY_DOCUMENT_TYPE", 80) }
        assertEquals("MY_TYPE", seal.documentType)
        assertEquals(listOf(FormatFinding.MissingRequired(listOf(0x91), "MY_NUMBER")), seal.formatFindings)
    }

    @Test
    fun unmarkedType_doesNotIdentifyTheDocumentType() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "NATIONAL_DOCUMENT_IDENTIFIER", "tag": 134, "coding": "BYTE"}]"""
        )
        val seal = seal("D<<") {
            addMessage("DOCUMENT_NUMBER", "ZZ1234567")
            addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x10)
        }
        assertEquals("DOCUMENT_NUMBER, NATIONAL_DOCUMENT_IDENTIFIER", seal.documentType)
        assertEquals(emptyList(), seal.formatFindings)
    }
}
