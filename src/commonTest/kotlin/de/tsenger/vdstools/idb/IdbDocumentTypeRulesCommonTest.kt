package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * The rules of a national IDB document type come from its data, like for VDS: required messages are
 * checked only if the type is verified against a `specification`, other messages are not allowed only
 * if `additionalMessagesAllowed` is false.
 */
class IdbDocumentTypeRulesCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun seal(block: IdbSeal.Builder.() -> Unit): IdbSeal {
        val built = IdbSeal.Builder().countryIdentifier("D<<").apply(block).build()
        return SealParser().parse(built.rawString) as IdbSeal
    }

    @Test
    fun bundledGermanTypes_areVerifiedAndClosed() {
        for (name in DataEncoder.idbDocumentTypes.availableDocumentTypes()) {
            val tag = DataEncoder.idbDocumentTypes.getDocumentType(name)!!
            val dto = DataEncoder.idbDocumentTypes.findDocumentType(tag, "D")!!
            assertTrue(dto.specification!!.startsWith("BSI TR-03137-1 V3.0 §1"), name)
            assertFalse(dto.additionalMessagesAllowed, name)
        }
    }

    private fun addCustomType(rules: String) = DataEncoder.addCustomIdbDocumentTypes(
        """[{"tag": 16, "name": "CUSTOM_PERMANENT_RESIDENCE", "country": "D", $rules
             "messages": [{"name": "DOCUMENT_REFERENCE", "required": true},
                          {"name": "DOCUMENT_NUMBER", "required": true},
                          {"name": "NATIONAL_DOCUMENT_IDENTIFIER", "required": true}]}]"""
    )

    /** DOCUMENT_NUMBER missing, CAN not listed. */
    private fun incompleteSeal() = seal {
        addMessage("DOCUMENT_REFERENCE", "ABC123456")
        addMessage("CAN", "123456")
        addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x10)
    }

    @Test
    fun withoutSpecification_nothingIsChecked() {
        addCustomType("")
        val seal = incompleteSeal()
        assertEquals("CUSTOM_PERMANENT_RESIDENCE", seal.documentType)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun withSpecification_requiredMessagesAreChecked() {
        addCustomType(""""specification": "Test spec §1",""")
        assertEquals(
            listOf(FormatFinding.MissingRequired(listOf(0x85), "DOCUMENT_NUMBER")),
            incompleteSeal().formatFindings
        )
    }

    @Test
    fun closedType_otherMessagesAreNotAllowed() {
        addCustomType(""""specification": "Test spec §1", "additionalMessagesAllowed": false,""")
        assertEquals(
            listOf(
                FormatFinding.MissingRequired(listOf(0x85), "DOCUMENT_NUMBER"),
                FormatFinding.NotAllowed(listOf(0x09), "CAN"),
            ),
            incompleteSeal().formatFindings
        )
    }
}
