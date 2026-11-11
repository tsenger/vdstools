package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * The message type that carries the national document type is marked in the data
 * (`identifiesDocumentType`, BSI TR-03137: 0x86 NATIONAL_DOCUMENT_IDENTIFIER), not in the code. The
 * document type is looked up for the country of that message type's definition.
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

    private fun addFrenchDocumentTypes() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "FR_DOCUMENT_TYPE", "tag": 144, "coding": "BYTE", "country": "F", "identifiesDocumentType": true},
                {"name": "FR_NUMBER", "tag": 145, "coding": "C40", "country": "F"}]"""
        )
        DataEncoder.addCustomIdbDocumentTypes(
            """[{"tag": 1, "name": "FR_TITRE", "country": "F",
                 "messages": [{"name": "FR_DOCUMENT_TYPE", "required": true}, {"name": "FR_NUMBER", "required": true}]}]"""
        )
    }

    @Test
    fun anotherCountryMarksItsOwnTag() {
        addFrenchDocumentTypes()
        val seal = seal("F<<") {
            addMessage("FR_DOCUMENT_TYPE", 1)
            addMessage("FR_NUMBER", "AB123")
        }
        assertEquals("FR_TITRE", seal.documentType)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun documentTypeIsCheckedForTheCountryOfTheIdentifier() {
        // tag 1 exists for D (SUBSTITUTE_IDENTITY_DOCUMENT) and F (FR_TITRE)
        addFrenchDocumentTypes()
        val seal = seal("F<<") { addMessage("FR_DOCUMENT_TYPE", 1) }
        assertEquals("FR_TITRE", seal.documentType)
        assertEquals(listOf(FormatFinding.MissingRequired(listOf(0x91), "FR_NUMBER")), seal.formatFindings)
    }

    @Test
    fun unmarkedType_doesNotIdentifyTheDocumentType() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "NATIONAL_DOCUMENT_IDENTIFIER", "tag": 134, "coding": "BYTE", "country": "D"}]"""
        )
        val seal = seal("D<<") {
            addMessage("DOCUMENT_NUMBER", "ZZ1234567")
            addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x10)
        }
        assertEquals("DOCUMENT_NUMBER, NATIONAL_DOCUMENT_IDENTIFIER", seal.documentType)
        assertEquals(emptyList(), seal.formatFindings)
    }
}
