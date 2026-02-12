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

    // --- ExtendedMessageDefinitions (JSON) ---

    @Test
    fun testCustomExtendedMessageDefinitionsUsedDuringSealParsing() {
        DataEncoder.loadCustomExtendedMessageDefinitionsFromFile("CustomExtendedMessageDefinitions.json")

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

    // --- ExtendedMessageDefinitions (XML / TR-03171) ---

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

        DataEncoder.loadExtendedMessageDefinitionFromXml(xml)

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal

        assertEquals("XML_MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)
        assertEquals("Mustermann", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Berlin", seal.getMessage("CITY")?.value.toString())
    }

    // --- SealCodings ---

    @Test
    fun testCustomSealCodingsReplacesDefaults() {
        // CustomSealCodings.json only has documentRef "1234" -> CUSTOM_SEAL_CODING1
        // socialInsurance uses documentRef fc04, which won't be found
        DataEncoder.loadCustomSealCodingsFromFile("CustomSealCodings.json")

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("UNKNOWN", seal.documentType)
    }

    @Test
    fun testCustomSealCodingsWithRenamedType() {
        val customJson = """
            [{
                "documentType": "RENAMED_SOCIAL_CARD",
                "documentRef": "fc04",
                "version": 4,
                "messages": [
                    {"name": "SOCIAL_INSURANCE_NUMBER", "tag": 1, "coding": "C40", "decodedLength": 12, "required": true, "minLength": 8, "maxLength": 8},
                    {"name": "SURNAME", "tag": 2, "coding": "UTF8_STRING", "required": true, "minLength": 1, "maxLength": 90},
                    {"name": "FIRST_NAME", "tag": 3, "coding": "UTF8_STRING", "required": true, "minLength": 1, "maxLength": 90},
                    {"name": "BIRTH_NAME", "tag": 4, "coding": "UTF8_STRING", "required": false, "minLength": 1, "maxLength": 90}
                ]
            }]
        """.trimIndent()

        DataEncoder.loadCustomSealCodings(customJson)

        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("RENAMED_SOCIAL_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.value.toString())
        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.value.toString())
    }

    @Test
    fun testCustomSealCodingsWithRenamedMessages() {
        val customJson = """
            [{
                "documentType": "SOCIAL_INSURANCE_CARD",
                "documentRef": "fc04",
                "version": 4,
                "messages": [
                    {"name": "VERSICHERUNGSNUMMER", "tag": 1, "coding": "C40", "decodedLength": 12, "required": true, "minLength": 8, "maxLength": 8},
                    {"name": "NACHNAME", "tag": 2, "coding": "UTF8_STRING", "required": true, "minLength": 1, "maxLength": 90},
                    {"name": "VORNAME", "tag": 3, "coding": "UTF8_STRING", "required": true, "minLength": 1, "maxLength": 90},
                    {"name": "GEBURTSNAME", "tag": 4, "coding": "UTF8_STRING", "required": false, "minLength": 1, "maxLength": 90}
                ]
            }]
        """.trimIndent()

        DataEncoder.loadCustomSealCodings(customJson)

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
        DataEncoder.loadCustomIdbDocumentTypesFromFile("CustomIdbNationalDocumentTypes.json")

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
        DataEncoder.loadCustomIdbMessageTypesFromFile("CustomIdbMessageTypes.json")

        // Re-parse: messageList is computed on each access
        val messages = seal.messageList
        assertEquals(1, messages.size)
        assertEquals(4, messages[0].tag)
        // Tag 4 is not in custom types -> "UNKNOWN"
        assertEquals("UNKNOWN", messages[0].name)
    }
}