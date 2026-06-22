package de.tsenger.vdstools.vds

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.EcdsaSigner
import de.tsenger.vdstools.generic.MessageValue
import kotlinx.datetime.LocalDate
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import java.io.FileInputStream
import java.nio.file.FileSystems
import java.security.KeyStore
import java.security.Security
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreateTR03171Seals {

    @Test
    fun headerBuilderResolvesExtendedDefinitionToBaseDocumentRef() {
        val header = VdsHeader.Builder("MELDEBESCHEINIGUNG")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        assertEquals(
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c8",
            header.encoded.toHexString()
        )
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V8", header.vdsType)
    }

    @Test
    fun headerBuilderWithBaseTypeProducesSameEncoding() {
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS_V8")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        assertEquals(
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c8",
            header.encoded.toHexString()
        )
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V8", header.vdsType)
    }

    @Test
    fun buildMeldebescheinigungSealWithSignature() {
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS_V8")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        assertEquals(
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c8",
            header.encoded.toHexString()
        )
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V8", header.vdsType)

        val messageGroup = VdsMessageGroup.Builder("MELDEBESCHEINIGUNG")
            .addMessage("SURNAME", "Leiermann")
            .addMessage("FIRST_NAME", "Lorenzo")
            .build()

        assertEquals("Leiermann", messageGroup.getMessage(4).toString())
        assertEquals(
            "00109a4223406d374ef99e2cf95e31a2384604094c656965726d616e6e06074c6f72656e7a6f",
            messageGroup.encoded.toHexString()
        )


        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = EcdsaSigner(ecPrivKey.encoded, "brainpoolP256r1")

        val vdsSeal = VdsSeal(header, messageGroup, signer)

        assertEquals("MELDEBESCHEINIGUNG", vdsSeal.documentType)
        assertEquals("Lorenzo", vdsSeal.getMessage("FIRST_NAME").toString())
        assertNull(vdsSeal.getMessage(0))

        val encodedSealBytes = vdsSeal.encoded
        println("Encoded seal bytes: ${encodedSealBytes.toHexString()}")
        println("Raw String: ${DataEncoder.encodeBase256(encodedSealBytes)}")
        generateVdsDataMatrix(encodedSealBytes, "/tmp/vds.png")
    }

    @Test
    fun messageGroupBuilderWithValidityDates() {
        val messageGroup = VdsMessageGroup.Builder("MELDEBESCHEINIGUNG")
            .addMessage(
                "VALIDITY_DATES",
                MessageValue.ValidityDatesValue.of(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31))
            )
            .addMessage("SURNAME", "Leiermann")
            .build()

        // VALIDITY_DATES (tag 1) is a base-type metadata tag → only in metadataMessageList
        val validityMessage = messageGroup.metadataMessageList.firstOrNull { it.name == "VALIDITY_DATES" }
        assertNotNull(validityMessage, "VALIDITY_DATES message should be present in metadataMessageList")

        // Verify decoding as ValidityDatesValue
        val validityValue = validityMessage.value as MessageValue.ValidityDatesValue
        assertEquals(LocalDate(2025, 1, 1), validityValue.validFrom)
        assertEquals(LocalDate(2025, 12, 31), validityValue.validTo)

        // Verify surname still works
        assertEquals("Leiermann", messageGroup.getMessage("SURNAME").toString())
    }

    @Test
    fun messageGroupBuilderWithOnlyValidFrom() {
        val messageGroup = VdsMessageGroup.Builder("MELDEBESCHEINIGUNG")
            .addMessage("VALIDITY_DATES", MessageValue.ValidityDatesValue.of(LocalDate(2025, 6, 1), null))
            .addMessage("SURNAME", "Müller")
            .build()

        // VALIDITY_DATES (tag 1) is a base-type metadata tag → only in metadataMessageList
        val validityMessage = messageGroup.metadataMessageList.firstOrNull { it.name == "VALIDITY_DATES" }
        assertNotNull(validityMessage)

        val validityValue = validityMessage.value as MessageValue.ValidityDatesValue
        assertEquals(LocalDate(2025, 6, 1), validityValue.validFrom)
        assertNull(validityValue.validTo)
    }

    // -------------------------------------------------------------------------
    // TR-03171 v0.9 — ADMINISTRATIVE_DOCUMENTS_V9 (document category 0xC9)
    // -------------------------------------------------------------------------

    /**
     * Profile definition for v0.9 JVM tests. The UUID is intentionally different from the
     * Meldebescheinigung UUID so the two profiles do not interfere.
     */
    private val v9ProfileXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <profile>
            <profileNumber>CCDDAABB44332211CCDDAABB44332211</profileNumber>
            <profileName>TEST_V9_JVM_PROFILE</profileName>
            <creator>Test</creator>
            <entry tag="10">
                <name>SURNAME</name>
                <description>Familienname</description>
                <type>UTF8String</type>
            </entry>
            <entry tag="11" optional="true">
                <name>FIRST_NAME</name>
                <description>Vorname</description>
                <type>UTF8String</type>
            </entry>
        </profile>
    """.trimIndent()

    @Test
    fun headerBuilderV9_resolvesCategoryC9() {
        // Load a v0.9 profile so the header builder can resolve the base type
        DataEncoder.loadVdsProfileDefinitionFromXml(v9ProfileXml)

        val header = VdsHeader.Builder("TEST_V9_JVM_PROFILE")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference("00112233445566778899AABBCCDDEEFF")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        // Same encoding as the 0xC8 header except the last byte is 0xC9
        assertEquals(
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c9",
            header.encoded.toHexString()
        )
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", header.vdsType)
    }

    @Test
    fun buildV9SealWithMandatoryMetadataAndContentField() {
        DataEncoder.loadVdsProfileDefinitionFromXml(v9ProfileXml)

        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = EcdsaSigner(ecPrivKey.encoded, "brainpoolP256r1")

        val seal = VdsSeal.Builder("TEST_V9_JVM_PROFILE")
            .issuingCountry("D<<")
            .signerIdentifier("DEZV")
            .certificateReference("00112233445566778899AABBCCDDEEFF")
            .issuingDate(LocalDate.parse("2024-09-27"))
            .sigDate(LocalDate.parse("2024-09-27"))
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("SURNAME", "Mustermann")
            .addMessage("FIRST_NAME", "Erika")
            .build(signer)

        // Verify document type and base type
        assertEquals("TEST_V9_JVM_PROFILE", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V9", seal.baseDocumentType)

        // Verify content is in messageList
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Erika", seal.getMessageByName("FIRST_NAME")?.value.toString())

        // Verify metadata is in metadataMessageList, not messageList
        assertNull(seal.getMessageByName("PROFILE_URI"),
            "PROFILE_URI is metadata and must not be in messageList")
        assertNotNull(seal.metadataMessageList.firstOrNull { it.name == "PROFILE_URI" })
        assertNotNull(seal.metadataMessageList.firstOrNull { it.name == "CERTIFICATE_URI" })

        val encodedBytes = seal.encoded
        println("V9 seal bytes: ${encodedBytes.toHexString()}")
        println("V9 raw string: ${DataEncoder.encodeBase256(encodedBytes)}")
        generateVdsDataMatrix(encodedBytes, "/tmp/vds_v9.png")
    }

    @Test
    fun buildV9SealWithValidityDatesAndStatusUri() {
        DataEncoder.loadVdsProfileDefinitionFromXml(v9ProfileXml)

        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = EcdsaSigner(ecPrivKey.encoded, "brainpoolP256r1")

        val seal = VdsSeal.Builder("TEST_V9_JVM_PROFILE")
            .issuingCountry("D<<")
            .signerIdentifier("DEZV")
            .certificateReference("00112233445566778899AABBCCDDEEFF")
            .issuingDate(LocalDate.parse("2024-09-27"))
            .sigDate(LocalDate.parse("2024-09-27"))
            .addMessage("PROFILE_URI", "example.com/profiles")
            .addMessage("CERTIFICATE_URI", "example.com/certs")
            .addMessage("VALID_FROM", LocalDate(2025, 1, 1))
            .addMessage("VALID_TO", LocalDate(2025, 12, 31))
            .addMessage("STATUS_URI", "example.com/status")
            .addMessage("SURNAME", "Mustermann")
            .build(signer)

        // VALID_FROM and VALID_TO are metadata — decode as DateValue with 8-byte raw representation
        val validFrom = seal.metadataMessageList.firstOrNull { it.name == "VALID_FROM" }
        assertNotNull(validFrom)
        assertEquals(8, validFrom.value.rawBytes.size,
            "VALID_FROM must use DATE_STRING (8 bytes), not the 3-byte ICAO format")
        assertEquals("20250101", validFrom.value.rawBytes.decodeToString())

        val validTo = seal.metadataMessageList.firstOrNull { it.name == "VALID_TO" }
        assertNotNull(validTo)
        assertEquals("20251231", validTo.value.rawBytes.decodeToString())

        val statusUri = seal.metadataMessageList.firstOrNull { it.name == "STATUS_URI" }
        assertNotNull(statusUri)
        assertEquals("example.com/status", statusUri.value.toString())

        println("V9 seal with dates bytes: ${seal.encoded.toHexString()}")
    }

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/commonTest/resources/vdstools_testcerts.bks"
        lateinit var keystore: KeyStore

        @JvmStatic
        @BeforeClass

        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }

    }

    fun generateVdsDataMatrix(vdsData: ByteArray, filePath: String) {
        val width = 300
        val height = 300

        try {
            // Da ZXings Writer primär Strings nimmt, wandeln wir das ByteArray
            // in einen ISO-8859-1 String um, um die 8-Bit-Werte zu erhalten
            val contents = DataEncoder.encodeBase256(vdsData)

            val bitMatrix = MultiFormatWriter().encode(
                contents,
                BarcodeFormat.DATA_MATRIX,
                width,
                height
            )

            val path = FileSystems.getDefault().getPath(filePath)
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)

            println("DataMatrix erfolgreich erstellt: $filePath")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}