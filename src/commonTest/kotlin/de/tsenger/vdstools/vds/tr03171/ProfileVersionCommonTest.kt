package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.VdsProfileResolver
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

/**
 * A TR-03171 v0.8 and a v0.9 profile sharing UUID and profile name (an issuer moved its profile to
 * v0.9 and kept the UUID). 0xC8 seals must use the v0.8 profile, 0xC9 seals the v0.9 profile.
 */
class ProfileVersionCommonTest {

    private val uuid = "9a4223406d374ef99e2cf95e31a23846" // UUID of test_profile_v08_meldebestaetigung.xml

    private val v09ProfileXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <profile>
            <profileNumber>${uuid.uppercase()}</profileNumber>
            <versionTR>0.9</versionTR>
            <profileName>${TestProfiles.MELDEBESTAETIGUNG}</profileName>
            <validFromPresent>false</validFromPresent>
            <validToPresent>false</validToPresent>
            <entry tag="10"><name>residence</name><description>Wohnort</description><type>UTF8String</type></entry>
        </profile>
    """.trimIndent()

    private val dummySigner = object : Signer {
        override val fieldSize = 256
        override fun sign(data: ByteArray) = ByteArray(64)
    }

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
        TestProfiles.loadMeldebestaetigung()
        DataEncoder.loadVdsProfileDefinitionFromXml(v09ProfileXml)
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun VdsSeal.Builder.withHeader() = issuingCountry("D<<")
        .signerIdentifier("DEZV")
        .certificateReference("00112233445566778899AABBCCDDEEFF00112233")

    private fun buildV08Seal(): ByteArray = VdsSeal.Builder.forProfileUuid(uuid, TrVersion.V0_8).withHeader()
        .addMessage("familyName", "Mustermann")
        .build(dummySigner).encoded

    private fun buildV09Seal(): ByteArray = VdsSeal.Builder.forProfileUuid(uuid, TrVersion.V0_9).withHeader()
        .addMessage("PROFILE_URI", "example.com/profiles")
        .addMessage("CERTIFICATE_URI", "example.com/certs")
        .addMessage("residence", "Berlin")
        .build(dummySigner).encoded

    @Test
    fun bothProfilesAreRegistered() {
        val registry = DataEncoder.vdsProfileDefinitions
        assertEquals(12, registry.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8)?.messages?.size)
        assertEquals(1, registry.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)?.messages?.size)
        assertEquals(2, registry.definitions.size)
    }

    @Test
    fun parse_usesProfileMatchingSealVersion() {
        val v08 = VdsSeal.fromByteArray(buildV08Seal())
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8, v08.baseDocumentType)
        assertEquals(TestProfiles.MELDEBESTAETIGUNG, v08.documentType)
        assertEquals("Mustermann", v08.getMessageByName("familyName")?.value.toString())
        assertNull(v08.getMessageByName("residence"))

        val v09 = VdsSeal.fromByteArray(buildV09Seal())
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, v09.baseDocumentType)
        assertEquals(TestProfiles.MELDEBESTAETIGUNG, v09.documentType)
        assertEquals("Berlin", v09.getMessageByName("residence")?.value.toString())
        assertNull(v09.getMessageByName("familyName"))
    }

    @Test
    fun customResolver_receivesBaseTypeAndProfileUri() {
        val v08Bytes = buildV08Seal()
        val v09Bytes = buildV09Seal()
        val calls = mutableListOf<Pair<String?, String>>()
        DataEncoder.vdsProfileResolver = VdsProfileResolver { uuidBytes, profileUri, baseDocumentType ->
            calls += profileUri to baseDocumentType
            DataEncoder.defaultVdsProfileResolver.resolve(uuidBytes, profileUri, baseDocumentType)
        }

        VdsSeal.fromByteArray(v08Bytes)
        VdsSeal.fromByteArray(v09Bytes)

        assertEquals(
            listOf(
                null to DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8,
                "example.com/profiles" to DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9
            ),
            calls
        )
    }

    @Test
    fun build_forProfileUuid_selectsRequestedVersion() {
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8, VdsSeal.fromByteArray(buildV08Seal()).baseDocumentType)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, VdsSeal.fromByteArray(buildV09Seal()).baseDocumentType)
    }

    @Test
    fun build_withoutVersion_failsWhenAmbiguous() {
        val byUuid = assertFailsWith<IllegalArgumentException> { VdsSeal.Builder.forProfileUuid(uuid) }
        assertTrue(byUuid.message!!.contains("forProfileUuid"), byUuid.message)
        assertFailsWith<IllegalArgumentException> { VdsSeal.Builder(TestProfiles.MELDEBESTAETIGUNG) }
    }

    @Test
    fun build_withoutVersion_usesProfileWhenUnique() {
        DataEncoder.resetToDefaults()
        DataEncoder.loadVdsProfileDefinitionFromXml(v09ProfileXml)

        val byName = VdsSeal.Builder(TestProfiles.MELDEBESTAETIGUNG).withHeader()
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("residence", "Berlin")
            .build(dummySigner)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, byName.baseDocumentType)

        val byUuid = VdsSeal.Builder.forProfileUuid(uuid).withHeader()
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("residence", "Berlin")
            .build(dummySigner)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, byUuid.baseDocumentType)
    }

    @Test
    fun build_forProfileUuid_failsForUnregisteredVersion() {
        DataEncoder.resetToDefaults()
        DataEncoder.loadVdsProfileDefinitionFromXml(v09ProfileXml)
        assertFailsWith<IllegalArgumentException> { VdsSeal.Builder.forProfileUuid(uuid, TrVersion.V0_8) }
    }
}
