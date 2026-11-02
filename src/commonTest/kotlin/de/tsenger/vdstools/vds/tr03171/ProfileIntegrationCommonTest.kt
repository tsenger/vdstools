package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.EcdsaSigner
import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.SealParser
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

    @Test
    fun testLoadV09Profiles() {
        // file name -> (UUID, highest tag, validFromPresent/validToPresent); entries start at tag 10 in v0.9
        val profiles = mapOf(
            "test_profile_gewerbeerlaubnis.xml" to Triple("4f2b91c7a8e5402dbc3169d7e0a45f18", 18, true),
            "test_profile_meldebescheinigung.xml" to Triple("9a4223406d374ef99e2cf95e31a23846", 21, false),
        )
        profiles.forEach { (fileName, expected) ->
            val (uuid, lastTag, validityPresent) = expected
            DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource(fileName))

            val definition = DataEncoder.vdsProfileDefinitions.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)
            assertNotNull(definition, fileName)
            assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, definition.baseDocumentType, fileName)
            assertEquals((10..lastTag).toList(), definition.messages.map { it.tag }, fileName)
            assertEquals(validityPresent, definition.validFromPresent, fileName)
            assertEquals(validityPresent, definition.validToPresent, fileName)
        }
    }

    @Test
    fun testGewerbeerlaubnisV09ProfileCodings() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_gewerbeerlaubnis.xml"))
        val definition = DataEncoder.vdsProfileDefinitions.resolveByName("GEWERBEERLAUBNIS", DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)
        assertNotNull(definition)
        val messages = definition.messages.associateBy { it.name }

        assertEquals(MessageCoding.DATE_STRING, messages.getValue("DATE_OF_BIRTH").coding)
        assertFalse(messages.getValue("DATE_OF_BIRTH").required)
        assertEquals(MessageCoding.DATE_TIME_STRING, messages.getValue("ISSUED_AT").coding)
        assertTrue(messages.getValue("ISSUED_AT").required)
        assertEquals(MessageCoding.INTEGER, messages.getValue("NUMBER_OF_EMPLOYEES").coding)
        assertFalse(messages.getValue("NUMBER_OF_EMPLOYEES").required)
        assertEquals(5, messages.getValue("POSTAL_CODE").maxBytes)
    }

    @Test
    fun testGewerbeerlaubnisV09SealRoundtrip() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_gewerbeerlaubnis.xml"))
        val keyPair = CryptographyProvider.Default.get(ECDSA).keyPairGenerator(EC.Curve.P256).generateKeyBlocking()
        val signer = EcdsaSigner(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "prime256v1")

        val seal = VdsSeal.Builder("GEWERBEERLAUBNIS")
            .issuingCountry("D<<")
            .signerIdentifier("DEZV")
            .certificateReference("00112233445566778899AABBCCDDEEFF00112233")
            .issuingDate(LocalDate(2025, 5, 14))
            .sigDate(LocalDate(2025, 5, 14))
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("VALID_FROM", LocalDate(2025, 6, 1))
            .addMessage("VALID_TO", LocalDate(2030, 5, 31))
            .addMessage("SURNAME", "Mustermann")
            .addMessage("FIRST_NAME", "Erika")
            .addMessage("DATE_OF_BIRTH", LocalDate(1964, 8, 12))
            .addMessage("BUSINESS_NAME", "Bäckerei Mustermann")
            .addMessage("BUSINESS_ADDRESS", "Heidestraße 17, Köln")
            .addMessage("POSTAL_CODE", "51147")
            .addMessage("PERMIT_NUMBER", "GE-2025-0815")
            .addMessage("ISSUED_AT", LocalDateTime(2025, 5, 14, 9, 30, 15))
            .addMessage("NUMBER_OF_EMPLOYEES", 12)
            .build(signer)

        val parsed = SealParser().parse(seal.encoded)

        assertEquals("GEWERBEERLAUBNIS", parsed.documentType)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, parsed.baseDocumentType)
        assertIs<VdsSeal>(parsed)
        assertEquals(0xc9.toByte(), parsed.docTypeCat)
        assertEquals("4f2b91c7a8e5402dbc3169d7e0a45f18", parsed.documentProfileUuid?.toHexString())

        val validFrom = parsed.metadataMessageList.first { it.name == "VALID_FROM" }.value
        assertIs<MessageValue.DateValue>(validFrom)
        assertEquals(LocalDate(2025, 6, 1), validFrom.date)
        val validTo = parsed.metadataMessageList.first { it.name == "VALID_TO" }.value
        assertIs<MessageValue.DateValue>(validTo)
        assertEquals(LocalDate(2030, 5, 31), validTo.date)

        assertEquals(9, parsed.messageList.size)
        assertEquals("Mustermann", parsed.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Bäckerei Mustermann", parsed.getMessageByName("BUSINESS_NAME")?.value.toString())
        assertEquals("51147", parsed.getMessageByName("POSTAL_CODE")?.value.toString())
        val birth = parsed.getMessageByName("DATE_OF_BIRTH")?.value
        assertIs<MessageValue.DateValue>(birth)
        assertEquals(LocalDate(1964, 8, 12), birth.date)
        assertEquals("19640812", birth.rawBytes.decodeToString())
        val issuedAt = parsed.getMessageByName("ISSUED_AT")?.value
        assertIs<MessageValue.DateTimeValue>(issuedAt)
        assertEquals(LocalDateTime(2025, 5, 14, 9, 30, 15), issuedAt.dateTime)
        assertEquals("20250514093015", issuedAt.rawBytes.decodeToString())
        assertEquals("12", parsed.getMessageByName("NUMBER_OF_EMPLOYEES")?.value.toString())

        val publicKey = keyPair.publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.DER)
        assertEquals(Verifier.Result.SignatureValid, Verifier(parsed, publicKey, "prime256v1").verify())
    }

    @Test
    fun testGewerbeerlaubnisV09RequiresValidityDates() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_gewerbeerlaubnis.xml"))
        val keyPair = CryptographyProvider.Default.get(ECDSA).keyPairGenerator(EC.Curve.P256).generateKeyBlocking()
        val signer = EcdsaSigner(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "prime256v1")

        // validFromPresent/validToPresent = true -> a seal without VALID_FROM/VALID_TO is rejected
        val e = assertFailsWith<IllegalArgumentException> {
            VdsSeal.Builder("GEWERBEERLAUBNIS")
                .issuingCountry("D<<")
                .signerIdentifier("DEZV")
                .certificateReference("00112233445566778899AABBCCDDEEFF00112233")
                .addMessage("PROFILE_URI", "example.com/profiles")
                .addMessage("CERTIFICATE_URI", "example.com/certs")
                .addMessage("SURNAME", "Mustermann")
                .addMessage("FIRST_NAME", "Erika")
                .addMessage("BUSINESS_NAME", "Bäckerei Mustermann")
                .addMessage("BUSINESS_ADDRESS", "Heidestraße 17, Köln")
                .addMessage("POSTAL_CODE", "51147")
                .addMessage("PERMIT_NUMBER", "GE-2025-0815")
                .addMessage("ISSUED_AT", LocalDateTime(2025, 5, 14, 9, 30, 15))
                .build(signer)
        }
        assertTrue(e.message!!.contains("VALID_FROM"), e.message)
    }

    @Test
    fun testMeldebescheinigungV09SealRoundtrip() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_meldebescheinigung.xml"))
        val keyPair = CryptographyProvider.Default.get(ECDSA).keyPairGenerator(EC.Curve.P256).generateKeyBlocking()
        val signer = EcdsaSigner(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "prime256v1")

        // optional entries (ACADEMIC_DEGREE, COMMON_FIRST_NAME, DATE_OF_BIRTH) are omitted
        val seal = VdsSeal.Builder("MELDEBESCHEINIGUNG")
            .issuingCountry("D<<")
            .signerIdentifier("DEZV")
            .certificateReference("00112233445566778899AABBCCDDEEFF00112233")
            .issuingDate(LocalDate(2025, 5, 14))
            .sigDate(LocalDate(2025, 5, 14))
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("SURNAME", "Mustermann")
            .addMessage("FIRST_NAME", "Erika")
            .addMessage("STREET", "Heidestraße")
            .addMessage("HOUSE_NUMBER", "17")
            .addMessage("POSTAL_CODE", "51147")
            .addMessage("CITY", "Köln")
            .addMessage("MOVING_DATE", "20250414")
            .addMessage("HOUSING_STATUS", 1)
            .addMessage("DATE_OF_NOTIFICATION", "20250504")
            .build(signer)

        val parsed = SealParser().parse(seal.encoded)

        assertEquals("MELDEBESCHEINIGUNG", parsed.documentType)
        assertEquals(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9, parsed.baseDocumentType)
        assertIs<VdsSeal>(parsed)
        assertEquals(0xc9.toByte(), parsed.docTypeCat)
        assertTrue(parsed.metadataMessageList.none { it.name == "VALID_FROM" || it.name == "VALID_TO" })

        assertEquals(9, parsed.messageList.size)
        assertNull(parsed.getMessageByName("ACADEMIC_DEGREE"))
        assertEquals("Mustermann", parsed.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Köln", parsed.getMessageByName("CITY")?.value.toString())
        assertEquals("20250414", parsed.getMessageByName("MOVING_DATE")?.value.toString())
        val housingStatus = parsed.getMessageByName("HOUSING_STATUS")?.value
        assertIs<MessageValue.IntegerValue>(housingStatus)
        assertEquals("1", housingStatus.toString())

        val publicKey = keyPair.publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.DER)
        assertEquals(Verifier.Result.SignatureValid, Verifier(parsed, publicKey, "prime256v1").verify())
    }

    @Test
    fun testV08AndV09ProfileWithSameUuidCoexist() {
        // Meldebestätigung (v0.8) and Meldebescheinigung (v0.9) share the profile number
        TestProfiles.loadMeldebestaetigung()
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_meldebescheinigung.xml"))

        val uuid = "9a4223406d374ef99e2cf95e31a23846"
        assertEquals(TestProfiles.MELDEBESTAETIGUNG, DataEncoder.vdsProfileDefinitions.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8)?.definitionName)
        assertEquals("MELDEBESCHEINIGUNG", DataEncoder.vdsProfileDefinitions.resolve(uuid, DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)?.definitionName)

        // an existing v0.8 seal still resolves to the v0.8 profile
        val v08Seal = SealParser().parse(VdsRawBytesCommon.meldebescheinigung)
        assertEquals(TestProfiles.MELDEBESTAETIGUNG, v08Seal.documentType)
    }
}
