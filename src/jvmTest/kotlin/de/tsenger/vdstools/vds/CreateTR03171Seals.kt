package de.tsenger.vdstools.vds

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
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
        assertEquals("ADMINISTRATIVE_DOCUMENTS", header.vdsType)
    }

    @Test
    fun headerBuilderWithBaseTypeProducesSameEncoding() {
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS")
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
        assertEquals("ADMINISTRATIVE_DOCUMENTS", header.vdsType)
    }

    @Test
    fun buildMeldebescheinigungSealWithSignature() {
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS")
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
        assertEquals("ADMINISTRATIVE_DOCUMENTS", header.vdsType)

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
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")

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

        // Verify VALIDITY_DATES tag (1) was created
        val validityMessage = messageGroup.getMessage("VALIDITY_DATES")
        assertNotNull(validityMessage, "VALIDITY_DATES message should be present")

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

        val validityMessage = messageGroup.getMessage("VALIDITY_DATES")
        assertNotNull(validityMessage)

        val validityValue = validityMessage.value as MessageValue.ValidityDatesValue
        assertEquals(LocalDate(2025, 6, 1), validityValue.validFrom)
        assertNull(validityValue.validTo)
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