package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.generic.MessageCoding
import kotlin.test.*

class ProfileConverterCommonTest {

    private fun createProfile(vararg entries: ProfileEntryDto): ProfileDto {
        return ProfileDto(
            profileNumber = "9A4223406D374EF99E2CF95E31A23846",
            profileName = "TEST_PROFILE",
            creator = "Test",
            entries = entries.toList()
        )
    }

    private fun createEntry(
        tag: Int = 4,
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
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals("9a4223406d374ef99e2cf95e31a23846", result.definitionId)
    }

    @Test
    fun testProfileNameMappedToDefinitionName() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals("TEST_PROFILE", result.definitionName)
    }

    @Test
    fun testBaseDocumentTypeIsAdministrativeDocuments() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", result.baseDocumentType)
    }

    @Test
    fun testVersionIsOne() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals(1, result.version)
    }

    @Test
    fun testBooleanMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.BOOLEAN))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTE, msg.coding)
        assertEquals(1, msg.maxLength)
    }

    @Test
    fun testIntegerLength1MappedToByte() {
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER, length = 1))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTE, msg.coding)
        assertEquals(1, msg.maxLength)
    }

    @Test
    fun testIntegerNoLengthMappedToBytes() {
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(255, msg.maxLength)
    }

    @Test
    fun testIntegerWithLengthMappedToBytes() {
        val profile = createProfile(createEntry(type = Asn1Type.INTEGER, length = 4))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(4, msg.maxLength)
    }

    @Test
    fun testOctetStringMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.OCTET_STRING, length = 20))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(20, msg.maxLength)
    }

    @Test
    fun testOctetStringNoLengthMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.OCTET_STRING))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(255, msg.maxLength)
    }

    @Test
    fun testUtf8StringMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.UTF8String, length = 50))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.UTF8_STRING, msg.coding)
        assertEquals(50, msg.maxLength)
    }

    @Test
    fun testUtf8StringNoLengthMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.UTF8String))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.UTF8_STRING, msg.coding)
        assertEquals(255, msg.maxLength)
    }

    @Test
    fun testDateMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.DATE))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.DATE, msg.coding)
        assertEquals(3, msg.maxLength)
    }

    @Test
    fun testDateTimeMapping() {
        val profile = createProfile(createEntry(type = Asn1Type.DATE_TIME))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        val msg = result.messages[0]
        assertEquals(MessageCoding.BYTES, msg.coding)
        assertEquals(6, msg.maxLength)
    }

    @Test
    fun testOptionalTrueInvertedToRequiredFalse() {
        val profile = createProfile(createEntry(optional = true))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertFalse(result.messages[0].required)
    }

    @Test
    fun testOptionalFalseInvertedToRequiredTrue() {
        val profile = createProfile(createEntry(optional = false))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertTrue(result.messages[0].required)
    }

    @Test
    fun testMinLengthAlwaysOne() {
        val profile = createProfile(createEntry())
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals(1, result.messages[0].minLength)
    }

    @Test
    fun testTagMapping() {
        val profile = createProfile(createEntry(tag = 42))
        val result = ProfileConverter.toExtendedMessageDefinition(profile)
        assertEquals(42, result.messages[0].tag)
    }

    @Test
    fun testMeldebescheinigungRoundtrip() {
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
                <entry tag="14">
                    <name>HOUSING_STATUS</name>
                    <description>Wohnungsstatus</description>
                    <length>1</length>
                    <type>INTEGER</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)
        val definition = ProfileConverter.toExtendedMessageDefinition(profile)

        assertEquals("9a4223406d374ef99e2cf95e31a23846", definition.definitionId)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", definition.baseDocumentType)
        assertEquals(3, definition.messages.size)

        val surname = definition.messages.first { it.name == "SURNAME" }
        assertEquals(4, surname.tag)
        assertEquals(MessageCoding.UTF8_STRING, surname.coding)
        assertTrue(surname.required)
        assertEquals(255, surname.maxLength)

        val academic = definition.messages.first { it.name == "ACADEMIC_DEGREE" }
        assertFalse(academic.required)

        val housing = definition.messages.first { it.name == "HOUSING_STATUS" }
        assertEquals(MessageCoding.BYTE, housing.coding)
        assertEquals(1, housing.maxLength)
    }
}
