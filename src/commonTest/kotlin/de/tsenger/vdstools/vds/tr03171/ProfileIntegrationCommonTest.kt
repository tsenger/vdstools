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
                <profileName>TEST_XML_PROFILE</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>FIELD1</name>
                    <description>Test field</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        DataEncoder.loadExtendedMessageDefinitionFromXml(xml)

        val definition = DataEncoder.resolveExtendedMessageDefinition("aabbccdd11223344aabbccdd11223344")

        assertNotNull(definition)
        assertEquals("TEST_XML_PROFILE", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", definition.baseDocumentType)
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
                <profileName>NEW_PROFILE</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>FIELD1</name>
                    <description>Test</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        // Existing JSON definition should be present
        val existingDef = DataEncoder.resolveExtendedMessageDefinition("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(existingDef)
        assertEquals("MELDEBESCHEINIGUNG", existingDef.definitionName)

        // Load new XML definition
        DataEncoder.loadExtendedMessageDefinitionFromXml(xml)

        // Existing definition should still be there
        val stillExisting = DataEncoder.resolveExtendedMessageDefinition("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(stillExisting)
        assertEquals("MELDEBESCHEINIGUNG", stillExisting.definitionName)

        // New definition should also be resolvable
        val newDef = DataEncoder.resolveExtendedMessageDefinition("aabbccdd11223344aabbccdd11223344")
        assertNotNull(newDef)
        assertEquals("NEW_PROFILE", newDef.definitionName)
    }

    @Test
    fun testMeldebescheinigungXmlMatchesJson() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>MELDEBESCHEINIGUNG</profileName>
                <creator>BSI</creator>
                <entry tag="4">
                    <name>SURNAME</name>
                    <description>Familienname</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="5" optional="true">
                    <name>ACADEMIC_DEGREE</name>
                    <description>Akademischer Grad</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="6">
                    <name>FIRST_NAME</name>
                    <description>Vorname</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="7" optional="true">
                    <name>COMMON_FIRST_NAME</name>
                    <description>Rufname</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="8" optional="true">
                    <name>DATE_OF_BIRTH</name>
                    <description>Geburtsdatum</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="9">
                    <name>STREET</name>
                    <description>Strasse</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="10">
                    <name>HOUSE_NUMBER</name>
                    <description>Hausnummer</description>
                    <length>10</length>
                    <type>UTF8String</type>
                </entry>
                <entry tag="11">
                    <name>POSTAL_CODE</name>
                    <description>Postleitzahl</description>
                    <length>5</length>
                    <type>UTF8String</type>
                </entry>
                <entry tag="12">
                    <name>CITY</name>
                    <description>Ort</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="13">
                    <name>MOVING_DATE</name>
                    <description>Datum des Einzugs</description>
                    <length>8</length>
                    <type>UTF8String</type>
                </entry>
                <entry tag="14">
                    <name>HOUSING_STATUS</name>
                    <description>Wohnungsstatus</description>
                    <length>1</length>
                    <type>INTEGER</type>
                </entry>
                <entry tag="15">
                    <name>DATE_OF_NOTIFICATION</name>
                    <description>Datum der Anmeldung</description>
                    <length>8</length>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        // Get existing JSON-based definition
        val jsonDef = DataEncoder.resolveExtendedMessageDefinition("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(jsonDef)

        // Load XML and overwrite
        DataEncoder.loadExtendedMessageDefinitionFromXml(xml)
        val xmlDef = DataEncoder.resolveExtendedMessageDefinition("9a4223406d374ef99e2cf95e31a23846")
        assertNotNull(xmlDef)

        // Compare
        assertEquals(jsonDef.definitionName, xmlDef.definitionName)
        assertEquals(jsonDef.baseDocumentType, xmlDef.baseDocumentType)
        assertEquals(jsonDef.version, xmlDef.version)
        assertEquals(jsonDef.messages.size, xmlDef.messages.size)

        for (i in jsonDef.messages.indices) {
            val jsonMsg = jsonDef.messages[i]
            val xmlMsg = xmlDef.messages[i]
            assertEquals(jsonMsg.name, xmlMsg.name, "Message name mismatch at index $i")
            assertEquals(jsonMsg.tag, xmlMsg.tag, "Tag mismatch for ${jsonMsg.name}")
            assertEquals(jsonMsg.coding, xmlMsg.coding, "Coding mismatch for ${jsonMsg.name}")
            assertEquals(jsonMsg.required, xmlMsg.required, "Required mismatch for ${jsonMsg.name}")
            assertEquals(jsonMsg.maxBytes, xmlMsg.maxBytes, "MaxBytes mismatch for ${jsonMsg.name}")
        }
    }
}
