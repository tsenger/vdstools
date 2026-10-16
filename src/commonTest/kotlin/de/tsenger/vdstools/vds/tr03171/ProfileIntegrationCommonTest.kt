package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.EcdsaSigner
import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.SealParser
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.vds.VdsSeal
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.datetime.LocalDate
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

        val definition = DataEncoder.vdsProfileDefinitions.resolve("aabbccdd11223344aabbccdd11223344", DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)

        assertNotNull(definition)
        assertEquals("TEST_XML_PROFILE", definition.definitionName)
        // Default is V9 (0xC9) — the current TR-03171 standard
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", definition.baseDocumentType)
        assertEquals(1, definition.messages.size)
        assertEquals("FIELD1", definition.messages[0].name)
        assertEquals(MessageCoding.UTF8_STRING, definition.messages[0].coding)
    }

    @Test
    fun testLoadingSecondXmlProfileKeepsFirst() {
        TestProfiles.loadMeldebestaetigung()
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

        DataEncoder.loadVdsProfileDefinitionFromXml(xml)

        val existing = DataEncoder.vdsProfileDefinitions.resolve("9a4223406d374ef99e2cf95e31a23846", DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8)
        assertEquals(TestProfiles.MELDEBESTAETIGUNG, existing?.definitionName)
        val newDef = DataEncoder.vdsProfileDefinitions.resolve("aabbccdd11223344aabbccdd11223344", DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)
        assertEquals("NEW_PROFILE", newDef?.definitionName)
    }

    @Test
    fun testNoProfilesBundled() {
        assertTrue(DataEncoder.vdsProfileDefinitions.definitions.isEmpty())
    }

    @Test
    fun testLoadV08Profiles() {
        // file name -> (UUID, number of entries, highest tag); entries start at tag 4 in v0.8
        val profiles = mapOf(
            "test_profile_v08_pruefbescheinigung.xml" to Triple("b22a8387a2604b7591c08e5e8f0e0206", 11, 14),
            "test_profile_v08_bibliotheksausweis.xml" to Triple("d8c5e329263b4dafb55308ef7cab8e64", 9, 12),
            "test_profile_v08_anwohnerparkausweis.xml" to Triple("a3621edaef4548afa571ecde56b4f393", 7, 10),
            "test_profile_v08_meldebestaetigung.xml" to Triple("9a4223406d374ef99e2cf95e31a23846", 12, 15),
        )
        profiles.forEach { (fileName, expected) ->
            val (uuid, entryCount, lastTag) = expected
            DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource(fileName))

            val definition = DataEncoder.vdsProfileDefinitions.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8)
            assertNotNull(definition, fileName)
            assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8, definition.baseDocumentType, fileName)
            assertEquals(entryCount, definition.messages.size, fileName)
            assertEquals((4..lastTag).toList(), definition.messages.map { it.tag }, fileName)
            assertNull(definition.validFromPresent, fileName)
            assertNull(definition.validToPresent, fileName)
        }
    }

    @Test
    fun testPruefbescheinigungV08SealRoundtrip() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_v08_pruefbescheinigung.xml"))
        val keyPair = CryptographyProvider.Default.get(ECDSA).keyPairGenerator(EC.Curve.P256).generateKeyBlocking()
        val signer = EcdsaSigner(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "prime256v1")

        val seal = VdsSeal.Builder("Prüfbescheinigung")
            .issuingCountry("D<<")
            .signerIdentifier("DEZV")
            .certificateReference("00112233445566778899AABBCCDDEEFF00112233")
            .issuingDate(LocalDate(2025, 5, 14))
            .sigDate(LocalDate(2025, 5, 14))
            .addMessage("pruefstelle", "Prüfamt Musterstadt")
            .addMessage("pruefdatum", LocalDate(2025, 5, 14))
            .addMessage("nachname", "Mustermann")
            .addMessage("vorname", "Erika")
            .addMessage("geburtsdatum", "12.08.1964")
            .addMessage("geburtsort", "Berlin")
            .addMessage("pruefungsart", "Sachkunde")
            .addMessage("anzahlVersuche", 0)
            .addMessage("aktenzeichen", "PB-2025-0042")
            .addMessage("ergebnis", "Bestanden")
            .addMessage("bemerkung", "Keine")
            .build(signer)

        val parsed = SealParser().parse(seal.encoded)

        assertEquals("Prüfbescheinigung", parsed.documentType)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8, parsed.baseDocumentType)
        assertEquals(0xc8.toByte(), (parsed as VdsSeal).docTypeCat)
        assertEquals("Mustermann", parsed.getMessageByName("nachname")?.value.toString())
        assertEquals("12.08.1964", parsed.getMessageByName("geburtsdatum")?.value.toString())
        val issued = parsed.getMessageByName("pruefdatum")?.value
        assertIs<MessageValue.DateValue>(issued)
        assertEquals(LocalDate(2025, 5, 14), issued.date)
        assertEquals("0", parsed.getMessageByName("anzahlVersuche")?.value.toString())
        assertEquals("Bestanden", parsed.getMessageByName("ergebnis")?.value.toString())

        val publicKey = keyPair.publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.DER)
        assertEquals(Verifier.Result.SignatureValid, Verifier(parsed, publicKey, "prime256v1").verify())
    }
}
