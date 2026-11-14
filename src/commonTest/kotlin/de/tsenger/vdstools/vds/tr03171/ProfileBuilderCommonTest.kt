package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.VdsProfileResolver
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

/**
 * Ways to build a TR-03171 seal: by a profile the caller passes in (no registration), by a profile
 * name (must be unique; document types take precedence) and by UUID.
 */
class ProfileBuilderCommonTest {

    private fun profileXml(uuid: String, name: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <profile>
            <profileNumber>$uuid</profileNumber>
            <versionTR>0.9</versionTR>
            <profileName>$name</profileName>
            <validFromPresent>false</validFromPresent>
            <validToPresent>false</validToPresent>
            <entry tag="10"><name>SURNAME</name><description>Familienname</description><type>UTF8String</type></entry>
        </profile>
    """.trimIndent()

    private val uuidA = "AABBCCDD11223344AABBCCDD11223344"
    private val uuidB = "11111111222222223333333344444444"

    private val dummySigner = object : Signer {
        override val fieldSize = 256
        override fun sign(data: ByteArray) = ByteArray(64)
    }

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun VdsSeal.Builder.complete() = issuingCountry("D<<")
        .signerIdentifier("DEZV")
        .certificateReference("00112233445566778899AABBCCDDEEFF00112233")
        .addMessage("PROFILE_URI", "example.com/profiles")
        .addMessage("CERTIFICATE_URI", "example.com/certs")
        .addMessage("SURNAME", "Mustermann")

    @Test
    fun forProfile_buildsWithAnUnregisteredProfile() {
        val profile = ProfileConverter.toVdsProfileDefinition(ProfileXmlParser.parse(profileXml(uuidA, "OWN_PROFILE")))
        assertTrue(DataEncoder.vdsProfileDefinitions.definitions.isEmpty(), "the profile must not be registered")

        val seal = VdsSeal.Builder.forProfile(profile).complete().build(dummySigner)
        assertEquals("OWN_PROFILE", seal.documentType)
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())

        // an application that manages profiles itself reads the seal back through its resolver
        DataEncoder.vdsProfileResolver = VdsProfileResolver { uuid, _, base ->
            profile.takeIf { it.definitionId.equals(uuid.toHexString(), ignoreCase = true) && it.baseDocumentType == base }
        }
        val parsed = VdsSeal.fromByteArray(seal.encoded) as VdsSeal
        assertEquals("OWN_PROFILE", parsed.documentType)
        assertEquals("Mustermann", parsed.getMessageByName("SURNAME")?.value.toString())
    }

    @Test
    fun forProfile_baseTypeWithoutProfileNumber_throws() {
        val profile = ProfileConverter.toVdsProfileDefinition(ProfileXmlParser.parse(profileXml(uuidA, "OWN_PROFILE")))
        val e = assertFailsWith<IllegalArgumentException> { VdsSeal.Builder.forProfile(profile.copy(baseDocumentType = "VISA")) }
        assertTrue(e.message!!.contains("uuidMessageLookup"), e.message)
    }

    @Test
    fun byName_baseTypeWithoutProfileNumber_throws() {
        // the same rule as forProfile: the base type must carry the profile number
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml(uuidA, "OWN_PROFILE"))
        DataEncoder.addCustomVdsDocumentTypes("""[{"documentType": "ADMINISTRATIVE_DOCUMENTS_V9", "documentRef": "01c9", "version": 4}]""")
        val e = assertFailsWith<IllegalArgumentException> { VdsSeal.Builder("OWN_PROFILE") }
        assertTrue(e.message!!.contains("uuidMessageLookup"), e.message)
    }

    @Test
    fun forProfile_writesTheProfileNumberAtTheUuidTagOfTheBaseType() {
        DataEncoder.addCustomVdsDocumentTypes(
            """[{"documentType": "MY_BASE", "documentRef": "01ca", "version": 4, "uuidMessageLookup": true,
                 "uuidMessageTag": 5, "metadataTagList": [5]}]"""
        )
        val base = ProfileConverter.toVdsProfileDefinition(ProfileXmlParser.parse(profileXml(uuidA, "OWN_PROFILE")))
        val profile = base.copy(baseDocumentType = "MY_BASE")
        val seal = VdsSeal.Builder.forProfile(profile).addMessage("SURNAME", "Mustermann").build(dummySigner)
        assertEquals(uuidA, seal.documentProfileUuid?.toHexString()?.uppercase())
        assertEquals(listOf("SURNAME"), seal.messageList.map { it.name })
    }

    @Test
    fun byName_ambiguousProfileName_throws() {
        // a changed profile gets a new UUID but may keep its name
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml(uuidA, "MELDEBESTAETIGUNG"))
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml(uuidB, "MELDEBESTAETIGUNG"))

        val e = assertFailsWith<IllegalArgumentException> { VdsSeal.Builder("MELDEBESTAETIGUNG") }
        assertTrue(e.message!!.contains("forProfileUuid"), e.message)
        assertEquals("MELDEBESTAETIGUNG", VdsSeal.Builder.forProfileUuid(uuidB).complete().build(dummySigner).documentType)
    }

    @Test
    fun byName_uniqueProfileName_builds() {
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml(uuidA, "MELDEBESTAETIGUNG"))

        val seal = VdsSeal.Builder("MELDEBESTAETIGUNG").complete().build(dummySigner)
        assertEquals("MELDEBESTAETIGUNG", seal.documentType)
        assertEquals(uuidA, seal.documentProfileUuid?.toHexString()?.uppercase())
    }

    @Test
    fun byName_documentTypeTakesPrecedenceOverProfileOfTheSameName() {
        DataEncoder.loadVdsProfileDefinitionFromXml(profileXml(uuidA, "VISA"))

        val seal = VdsSeal.Builder("VISA")
            .addMessage("MRZ_MRVB", "V<D<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<T000007366D<<6408120F3012310")
            .addMessage("DURATION_OF_STAY", "5a0000".hexToByteArray())
            .addMessage("PASSPORT_NUMBER", "ALAAF5099")
            .build(dummySigner)
        assertEquals("VISA", seal.documentType)
        assertNull(seal.documentProfileUuid)
    }
}
