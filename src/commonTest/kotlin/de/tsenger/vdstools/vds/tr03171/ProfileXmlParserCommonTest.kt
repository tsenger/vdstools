package de.tsenger.vdstools.vds.tr03171

import kotlin.test.*

class ProfileXmlParserCommonTest {

    @Test
    fun testParseMinimalProfile() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>MINIMAL</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>FIELD1</name>
                    <description>A field</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)

        assertEquals("9A4223406D374EF99E2CF95E31A23846", profile.profileNumber)
        assertEquals("MINIMAL", profile.profileName)
        assertEquals("Test", profile.creator)
        assertNull(profile.category)
        assertNull(profile.leikaID)
        assertNull(profile.statusIndicator)
        assertEquals(1, profile.entries.size)
    }

    @Test
    fun testParseFullProfile() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>FULL</profileName>
                <creator>BSI</creator>
                <category>Meldewesen</category>
                <leikaID>99123456789012</leikaID>
                <statusIndicator>BLOCKLISTING</statusIndicator>
                <entry tag="4" optional="true">
                    <name>FIELD1</name>
                    <description>Description 1</description>
                    <length>10</length>
                    <type>UTF8String</type>
                    <defaultValue>default</defaultValue>
                </entry>
                <entry tag="5">
                    <name>FIELD2</name>
                    <description>Description 2</description>
                    <type>INTEGER</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)

        assertEquals("FULL", profile.profileName)
        assertEquals("BSI", profile.creator)
        assertEquals("Meldewesen", profile.category)
        assertEquals("99123456789012", profile.leikaID)
        assertEquals(StatusIndicator.BLOCKLISTING, profile.statusIndicator)
        assertEquals(2, profile.entries.size)

        val entry1 = profile.entries[0]
        assertEquals(4, entry1.tag)
        assertTrue(entry1.optional)
        assertEquals("FIELD1", entry1.name)
        assertEquals("Description 1", entry1.description)
        assertEquals(10, entry1.length)
        assertEquals(Asn1Type.UTF8String, entry1.type)
        assertEquals("default", entry1.defaultValue)

        val entry2 = profile.entries[1]
        assertEquals(5, entry2.tag)
        assertFalse(entry2.optional)
        assertEquals(Asn1Type.INTEGER, entry2.type)
        assertNull(entry2.length)
        assertNull(entry2.defaultValue)
    }

    @Test
    fun testOptionalDefaultsFalse() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>FIELD1</name>
                    <description>Desc</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)
        assertFalse(profile.entries[0].optional)
    }

    @Test
    fun testAllAsn1Types() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TYPES</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>F1</name>
                    <description>D</description>
                    <type>BOOLEAN</type>
                </entry>
                <entry tag="5">
                    <name>F2</name>
                    <description>D</description>
                    <type>INTEGER</type>
                </entry>
                <entry tag="6">
                    <name>F3</name>
                    <description>D</description>
                    <type>OCTET_STRING</type>
                </entry>
                <entry tag="7">
                    <name>F4</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="8">
                    <name>F5</name>
                    <description>D</description>
                    <type>DATE</type>
                </entry>
                <entry tag="9">
                    <name>F6</name>
                    <description>D</description>
                    <type>DATE-TIME</type>
                </entry>
            </profile>
        """.trimIndent()

        val profile = ProfileXmlParser.parse(xml)
        assertEquals(6, profile.entries.size)
        assertEquals(Asn1Type.BOOLEAN, profile.entries[0].type)
        assertEquals(Asn1Type.INTEGER, profile.entries[1].type)
        assertEquals(Asn1Type.OCTET_STRING, profile.entries[2].type)
        assertEquals(Asn1Type.UTF8String, profile.entries[3].type)
        assertEquals(Asn1Type.DATE, profile.entries[4].type)
        assertEquals(Asn1Type.DATE_TIME, profile.entries[5].type)
    }

    @Test
    fun testInvalidProfileNumber() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>INVALID</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>F1</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            ProfileXmlParser.parse(xml)
        }
        assertTrue(exception.message!!.contains("profileNumber"))
    }

    @Test
    fun testDuplicateTagsThrows() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
                <entry tag="4">
                    <name>F1</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
                <entry tag="4">
                    <name>F2</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            ProfileXmlParser.parse(xml)
        }
    }

    @Test
    fun testTagOutOfRangeThrows() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
                <entry tag="3">
                    <name>F1</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            ProfileXmlParser.parse(xml)
        }
        assertTrue(exception.message!!.contains("Tag 3 out of range"))
    }

    @Test
    fun testTag255OutOfRangeThrows() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
                <entry tag="255">
                    <name>F1</name>
                    <description>D</description>
                    <type>UTF8String</type>
                </entry>
            </profile>
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            ProfileXmlParser.parse(xml)
        }
    }

    @Test
    fun testNoEntriesThrows() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                <profileName>TEST</profileName>
                <creator>Test</creator>
            </profile>
        """.trimIndent()

        assertFailsWith<Exception> {
            ProfileXmlParser.parse(xml)
        }
    }

    @Test
    fun testAllStatusIndicators() {
        fun parseWithStatus(status: String): StatusIndicator? {
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <profile>
                    <profileNumber>9A4223406D374EF99E2CF95E31A23846</profileNumber>
                    <profileName>TEST</profileName>
                    <creator>Test</creator>
                    <statusIndicator>$status</statusIndicator>
                    <entry tag="4">
                        <name>F1</name>
                        <description>D</description>
                        <type>UTF8String</type>
                    </entry>
                </profile>
            """.trimIndent()
            return ProfileXmlParser.parse(xml).statusIndicator
        }

        assertEquals(StatusIndicator.NONE, parseWithStatus("NONE"))
        assertEquals(StatusIndicator.BLOCKLISTING, parseWithStatus("BLOCKLISTING"))
        assertEquals(StatusIndicator.ALLOWLISTING, parseWithStatus("ALLOWLISTING"))
    }
}
