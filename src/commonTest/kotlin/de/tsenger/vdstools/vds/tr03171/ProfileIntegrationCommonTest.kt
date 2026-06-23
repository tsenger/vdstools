package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.MessageCoding
import kotlin.test.*

class ProfileIntegrationCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @Test
    fun testLoadXmlAndResolveByUuid() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>AABBCCDD11223344AABBCCDD11223344</profileNumber>
                <versionTR>0.9</versionTR>
                <profileName>TEST_XML_PROFILE</profileName>
                <creator>Test</creator>
                <validFromPresent>false</validFromPresent>
                <validToPresent>false</validToPresent>
                <entry tag="10">
                    <name>FIELD1</name>
                    <description>Test field</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        DataEncoder.loadVdsProfileDefinitionFromXml(xml)

        val definition = DataEncoder.vdsProfileDefinitions.resolve("aabbccdd11223344aabbccdd11223344")

        assertNotNull(definition)
        assertEquals("TEST_XML_PROFILE", definition.definitionName)
        // Default is V9 (0xC9) — the current TR-03171 standard
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", definition.baseDocumentType)
        assertEquals(1, definition.messages.size)
        assertEquals("FIELD1", definition.messages[0].name)
        assertEquals(MessageCoding.UTF8_STRING, definition.messages[0].coding)
    }

    @Test
    fun testExistingJsonDefinitionsPreserved() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>AABBCCDD11223344AABBCCDD11223344</profileNumber>
                <versionTR>0.9</versionTR>
                <profileName>NEW_PROFILE</profileName>
                <creator>Test</creator>
                <validFromPresent>false</validFromPresent>
                <validToPresent>false</validToPresent>
                <entry tag="10">
                    <name>FIELD1</name>
                    <description>Test</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        // Existing JSON definition should be present
        val existingDef = DataEncoder.vdsProfileDefinitions.resolve("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(existingDef)
        assertEquals("MELDEBESCHEINIGUNG", existingDef.definitionName)

        // Load new XML definition
        DataEncoder.loadVdsProfileDefinitionFromXml(xml)

        // Existing definition should still be there
        val stillExisting = DataEncoder.vdsProfileDefinitions.resolve("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(stillExisting)
        assertEquals("MELDEBESCHEINIGUNG", stillExisting.definitionName)

        // New definition should also be resolvable
        val newDef = DataEncoder.vdsProfileDefinitions.resolve("aabbccdd11223344aabbccdd11223344")
        assertNotNull(newDef)
        assertEquals("NEW_PROFILE", newDef.definitionName)
    }

}
