package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.generic.MessageCoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileConverterCommonTest {

    private fun createProfile(vararg entries: ProfileEntryDto): ProfileDto {
        return ProfileDto(
            profileNumber = "9A4223406D374EF99E2CF95E31A23846",
            versionTR = "0.9",
            profileName = "TEST_PROFILE",
            creator = "Test",
            validFromPresent = false,
            validToPresent = false,
            entries = entries.toList()
        )
    }

    private fun createEntry(
        tag: Int = 10,
        type: Asn1Type = Asn1Type.UTF8String,
        length: Int? = null,
        optional: Boolean = false
    ): ProfileEntryDto {
        return ProfileEntryDto(
            tag = tag,
            optional = optional,
            name = "FIELD",
            description = "Desc",
            length = length,
            type = type
        )
    }

    @Test
    fun testProfileNumberMappedToDefinitionIdLowercase() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals("9a4223406d374ef99e2cf95e31a23846", result.definitionId)
    }

    @Test
    fun testProfileNameMappedToDefinitionName() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals("TEST_PROFILE", result.definitionName)
    }

    @Test
    fun testBaseDocumentTypeIsAlwaysV9() {
        // Profiles parsed from XML are always TR-03171 v0.9 (0xC9)
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", result.baseDocumentType)
    }

    @Test
    fun testProfileNameFallsBackToProfileNumberWhenAbsent() {
        val profile = ProfileDto(
            profileNumber = "9A4223406D374EF99E2CF95E31A23846",
            versionTR = "0.9",
            profileName = null,
            creator = null,
            validFromPresent = false,
            validToPresent = false,
            entries = listOf(createEntry())
        )
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals("9A4223406D374EF99E2CF95E31A23846", result.definitionName)
    }

    @Test
    fun testVersionIsOne() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals(1, result.version)
    }

    @Test
    fun testBooleanMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.BOOLEAN))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTE, msg.coding)
        assertEquals(1, msg.maxBytes)
    }

    @Test
    fun testIntegerLength1MappedToInteger() {
        // length is a validation constraint only; the coding stays INTEGER regardless of length
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER, length = 1))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.INTEGER, msg.coding)
        assertEquals(1, msg.maxBytes)
    }

    @Test
    fun testIntegerNoLengthMappedToInteger() {
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.INTEGER, msg.coding)
        assertEquals(255, msg.maxBytes)
    }

    @Test
    fun testIntegerWithLengthMappedToInteger() {
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER, length = 4))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.INTEGER, msg.coding)
        assertEquals(4, msg.maxBytes)
    }

    @Test
    fun testOctetStringMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.OCTET_STRING, length = 20))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(20, msg.maxBytes)
    }

    @Test
    fun testOctetStringNoLengthMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.OCTET_STRING))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(255, msg.maxBytes)
    }

    @Test
    fun testUtf8StringMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.UTF8String, length = 50))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.UTF8_STRING, msg.coding)
        assertEquals(50, msg.maxBytes)
    }

    @Test
    fun testUtf8StringNoLengthMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.UTF8String))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.UTF8_STRING, msg.coding)
        assertEquals(255, msg.maxBytes)
    }

    @Test
    fun testDateMapping() {
        // TR-03171 uses ASN.1 DATE as 8-byte YYYYMMDD UTF-8 (DATE_STRING), not the 3-byte ICAO binary format
        val profile = createProfile(createEntry(type = Asn1Type.DATE))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.DATE_STRING, msg.coding)
        assertEquals(8, msg.maxBytes)
    }

    @Test
    fun testDateTimeMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.DATE_TIME))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.DATE_TIME, msg.coding)
        assertEquals(6, msg.maxBytes)
    }

    @Test
    fun testOptionalTrueInvertedToRequiredFalse() {
        val profile = createProfile(createEntry(optional = true))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertFalse(result.messages[0].required)
    }

    @Test
    fun testOptionalFalseInvertedToRequiredTrue() {
        val profile = createProfile(createEntry(optional = false))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertTrue(result.messages[0].required)
    }

    @Test
    fun testMinLengthAlwaysOne() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals(1, result.messages[0].minBytes)
    }

    @Test
    fun testTagMapping() {
        val profile = createProfile(createEntry(tag = 42))
        val result = ProfileConverter.toVdsProfileDefinition(profile)
        assertEquals(42, result.messages[0].tag)
    }

    @Test
    fun testMeldebescheinigungRoundtrip() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <versionTR>0.9</versionTR>
                <profileName>MELDEBESCHEINIGUNG</profileName>
                <creator>BSI</creator>
                <validFromPresent>false</validFromPresent>
                <validToPresent>false</validToPresent>
                <entry tag="10">
                    <name>SURNAME</name>
                    <description>Familienname</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="11" optional="true">
                    <name>ACADEMIC_DEGREE</name>
                    <description>Akademischer Grad</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="20">
                    <name>HOUSING_STATUS</name>
                    <description>Wohnungsstatus</description>
                    <length>1</length>
                    <type>INTEGER</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)
        val definition = ProfileConverter.toVdsProfileDefinition(profile)

        assertEquals("9a4223406d374ef99e2cf95e31a23846", definition.definitionId)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", definition.baseDocumentType)
        assertEquals(3, definition.messages.size)

        val surname = definition.messages.first { it.name == "SURNAME" }
        assertEquals(10, surname.tag)
        assertEquals(MessageCoding.UTF8_STRING, surname.coding)
        assertTrue(surname.required)
        assertEquals(255, surname.maxBytes)

        val academic = definition.messages.first { it.name == "ACADEMIC_DEGREE" }
        assertFalse(academic.required)

        val housing = definition.messages.first { it.name == "HOUSING_STATUS" }
        assertEquals(MessageCoding.INTEGER, housing.coding)
        assertEquals(1, housing.maxBytes)
    }
}
