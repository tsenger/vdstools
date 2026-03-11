package de.tsenger.vdstools

import de.tsenger.vdstools.idb.IdbMessageGroup
import de.tsenger.vdstools.idb.IdbPayload
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.idb.IdbHeader
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class CustomRegistryIntegrationCommonTest {

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    // --- VdsProfileDefinitions (JSON) ---

    @Test
    fun testCustomVdsProfileDefinitionsUsedDuringSealParsing() {
        DataEncoder.replaceCustomVdsProfileDefinitionsFromFile("CustomVdsProfileDefinitions.json")

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal

        // Custom definition maps same UUID to "MY_CUSTOM_DOCUMENT"
        assertEquals("MY_CUSTOM_DOCUMENT", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)

        // Messages should still decode correctly (same structure)
        assertEquals("Mustermann", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Dr.", seal.getMessage("ACADEMIC_DEGREE")?.value.toString())
        assertEquals("Erika", seal.getMessage("FIRST_NAME")?.value.toString())
        assertEquals("20250414", seal.getMessage("MOVING_DATE")?.value.toString())
    }

    // --- VdsProfileDefinitions (XML / TR-03171) ---

    @Test
    fun testXmlProfileUsedDuringSealParsing() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>XML_MELDEBESCHEINIGUNG</profileName>
                <creator>Test</creator>
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

        DataEncoder.loadVdsProfileDefinitionFromXml(xml)

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal

        assertEquals("XML_MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)
        assertEquals("Mustermann", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Berlin", seal.getMessage("CITY")?.value.toString())
    }

    // --- VdsDocumentTypes ---

    @Test
    fun testCustomVdsDocumentTypesReplacesDefaults() {
        // CustomVdsDocumentTypes.json only has documentRef "1234" -> CUSTOM_SEAL_CODING1
        // socialInsurance uses documentRef fc04, which won't be found
        DataEncoder.replaceCustomVdsDocumentTypesFromFile("CustomVdsDocumentTypes.json")

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("UNKNOWN", seal.documentType)
    }

    @Test
    fun testCustomVdsDocumentTypesWithRenamedType() {
        val customJson = """
            [{
                "documentType": "RENAMED_SOCIAL_CARD",
                "documentRef": "fc04",
                "version": 4,
                "messages": [
                    {"name": "SOCIAL_INSURANCE_NUMBER", "tag": 1, "coding": "C40", "required": true, "minBytes": 8, "maxBytes": 8},
                    {"name": "SURNAME", "tag": 2, "coding": "UTF8_STRING", "required": true, "minBytes": 1, "maxBytes": 90},
                    {"name": "FIRST_NAME", "tag": 3, "coding": "UTF8_STRING", "required": true, "minBytes": 1, "maxBytes": 90},
                    {"name": "BIRTH_NAME", "tag": 4, "coding": "UTF8_STRING", "required": false, "minBytes": 1, "maxBytes": 90}
                ]
            }]
        """.trimIndent()

        DataEncoder.replaceCustomVdsDocumentTypes(customJson)

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("RENAMED_SOCIAL_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.value.toString())
        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.value.toString())
    }

    @Test
    fun testCustomVdsDocumentTypesWithRenamedMessages() {
        val customJson = """
            [{
                "documentType": "SOCIAL_INSURANCE_CARD",
                "documentRef": "fc04",
                "version": 4,
                "messages": [
                    {"name": "VERSICHERUNGSNUMMER", "tag": 1, "coding": "C40", "required": true, "minBytes": 8, "maxBytes": 8},
                    {"name": "NACHNAME", "tag": 2, "coding": "UTF8_STRING", "required": true, "minBytes": 1, "maxBytes": 90},
                    {"name": "VORNAME", "tag": 3, "coding": "UTF8_STRING", "required": true, "minBytes": 1, "maxBytes": 90},
                    {"name": "GEBURTSNAME", "tag": 4, "coding": "UTF8_STRING", "required": false, "minBytes": 1, "maxBytes": 90}
                ]
            }]
        """.trimIndent()

        DataEncoder.replaceCustomVdsDocumentTypes(customJson)

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        // Messages should now use German names
        assertEquals("65170839J003", seal.getMessage("VERSICHERUNGSNUMMER")?.value.toString())
        assertEquals("Perschweiß", seal.getMessage("NACHNAME")?.value.toString())
        assertEquals("Oscar", seal.getMessage("VORNAME")?.value.toString())
        // Original English names should no longer resolve
        assertNull(seal.getMessage("SOCIAL_INSURANCE_NUMBER"))
        assertNull(seal.getMessage("SURNAME"))
    }

    // --- IDB National Document Types ---

    @Test
    fun testCustomIdbDocumentTypesUsedDuringIdbSealParsing() {
        // certifyingPermanentResidence has NATIONAL_DOCUMENT_IDENTIFIER (tag 0x86) = 16
        // Default: tag 16 -> "CERTIFYING_PERMANENT_RESIDENCE"
        val certifyingPermanentResidence =
            "RDB1BNK6ADJL2PECXOABAHIMWCFMCAYQDQM3TI2XIGCCZ5EQDQM3TNUSIMAIQP5AIZJ6II7FQTQ2UNWUMTMXIXETVCSSXKBK7RFWGXX3JBLHXTPV26M2GBN42UWTEQB45P4C4X7JK5WI2VQW5IBV3YNDPHELTYIU54PQ4P4"

        // Verify default behavior first
        val sealDefault = IdbSeal.fromString(certifyingPermanentResidence) as IdbSeal
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", sealDefault.documentType)

        // Load custom types: only tag 1 -> CUSTOM1_DOCUMENT, tag 2 -> CUSTOM2_DOCUMENT
        DataEncoder.replaceCustomIdbDocumentTypesFromFile("CustomIdbDocumentTypes.json")

        val sealCustom = IdbSeal.fromString(certifyingPermanentResidence) as IdbSeal
        // Tag 16 is not in custom types -> "UNKNOWN"
        assertEquals("UNKNOWN", sealCustom.documentType)
    }

    // --- IDB Message Types ---

    @Test
    fun testCustomIdbMessageTypesUsedDuringIdbSealParsing() {
        // IDB payload with tag 4 (default: PROOF_OF_VACCINATION) containing 16 bytes
        val payload = IdbPayload.fromByteArray(
            "6abc61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray(),
            false
        )
        val seal = IdbSeal('A', payload)

        // Default: tag 4 -> PROOF_OF_VACCINATION
        assertEquals(1, seal.messageList.size)
        assertEquals("PROOF_OF_VACCINATION", seal.messageList[0].name)

        // Load custom types: only tag 1 -> MESSAGE_TYPE1, tag 2 -> MESSAGE_TYPE2
        DataEncoder.replaceCustomIdbMessageTypesFromFile("CustomIdbMessageTypes.json")

        // Re-parse: messageList is computed on each access
        val messages = seal.messageList
        assertEquals(1, messages.size)
        assertEquals("04", messages[0].tag)
        // Tag 4 is not in custom types -> "UNKNOWN"
        assertEquals("UNKNOWN", messages[0].name)
    }

    // --- addCustom*: merge tests ---

    @Test
    fun addCustomVdsDocumentTypes_keepsExistingEntries() {
        // CustomVdsDocumentTypes.json only has documentRef "1234" -> CUSTOM_SEAL_CODING1
        DataEncoder.addCustomVdsDocumentTypesFromFile("CustomVdsDocumentTypes.json")

        // Default entry still accessible
        assertEquals("SOCIAL_INSURANCE_CARD", DataEncoder.vdsDocumentTypes.getVdsType(0xfc04))
        // New entry also accessible
        assertEquals("CUSTOM_SEAL_CODING1", DataEncoder.vdsDocumentTypes.getVdsType(0x1234))
    }

    @Test
    fun addCustomVdsDocumentTypes_overwritesOnConflict() {
        val json = """[{
            "documentType": "RENAMED_SOCIAL_CARD",
            "documentRef": "fc04",
            "version": 4,
            "messages": [
                {"name": "SOCIAL_INSURANCE_NUMBER", "tag": 1, "coding": "C40"}
            ]
        }]"""

        DataEncoder.addCustomVdsDocumentTypes(json)

        // New entry wins for this documentRef
        assertEquals("RENAMED_SOCIAL_CARD", DataEncoder.vdsDocumentTypes.getVdsType(0xfc04))
    }

    @Test
    fun addCustomIdbMessageTypes_keepsExistingEntries() {
        // CustomIdbMessageTypes.json only has tags 1 and 2
        DataEncoder.addCustomIdbMessageTypesFromFile("CustomIdbMessageTypes.json")

        // Default entry still accessible
        assertEquals("PROOF_OF_VACCINATION", DataEncoder.idbMessageTypes.getMessageType(4))
        // New entries also accessible
        assertEquals("MESSAGE_TYPE1", DataEncoder.idbMessageTypes.getMessageType(1))
        assertEquals("MESSAGE_TYPE2", DataEncoder.idbMessageTypes.getMessageType(2))
    }

    @Test
    fun addCustomIdbMessageTypes_overwritesOnConflict() {
        val json = """[{"name": "MY_VACCINATION", "tag": 4, "coding": "BYTES"}]"""

        DataEncoder.addCustomIdbMessageTypes(json)

        assertEquals("MY_VACCINATION", DataEncoder.idbMessageTypes.getMessageType(4))
    }

    @Test
    fun addCustomIdbDocumentTypes_keepsExistingEntries() {
        // CustomIdbDocumentTypes.json only has tags 1 and 2
        DataEncoder.addCustomIdbDocumentTypesFromFile("CustomIdbDocumentTypes.json")

        // Default entry still accessible (tag 16)
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", DataEncoder.idbDocumentTypes.getDocumentType(16))
        // New entries also accessible
        assertEquals("CUSTOM1_DOCUMENT", DataEncoder.idbDocumentTypes.getDocumentType(1))
        assertEquals("CUSTOM2_DOCUMENT", DataEncoder.idbDocumentTypes.getDocumentType(2))
    }

    @Test
    fun addCustomVdsProfileDefinitions_keepsExistingEntries() {
        val newUuid = "aaaabbbbccccddddaaaabbbbccccdddd"
        val json = """[{
            "definitionId": "$newUuid",
            "definitionName": "NEW_DOCUMENT",
            "baseDocumentType": "ADMINISTRATIVE_DOCUMENTS",
            "version": 1,
            "messages": []
        }]"""

        DataEncoder.addCustomVdsProfileDefinitions(json)

        // Default MELDEBESCHEINIGUNG is still there
        val defaultDef = DataEncoder.vdsProfileDefinitions.resolve("9a4223406d374ef99e2cf95e31a23846")
        assertEquals("MELDEBESCHEINIGUNG", defaultDef?.definitionName)
        // New definition also accessible
        assertEquals("NEW_DOCUMENT", DataEncoder.vdsProfileDefinitions.resolve(newUuid)?.definitionName)
    }

    @Test
    fun addCustomVdsProfileDefinitions_overwritesOnConflict() {
        // CustomVdsProfileDefinitions.json maps the same UUID as MELDEBESCHEINIGUNG to MY_CUSTOM_DOCUMENT
        DataEncoder.addCustomVdsProfileDefinitionsFromFile("CustomVdsProfileDefinitions.json")

        val def = DataEncoder.vdsProfileDefinitions.resolve("9a4223406d374ef99e2cf95e31a23846")
        assertEquals("MY_CUSTOM_DOCUMENT", def?.definitionName)
    }
}