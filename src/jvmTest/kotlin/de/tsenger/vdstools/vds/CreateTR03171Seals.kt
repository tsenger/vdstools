package de.tsenger.vdstools.vds

import de.tsenger.vdstools.Signer
import kotlinx.datetime.LocalDate
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val cert: X509Certificate = keystore.getCertificate("utts5b") as X509Certificate

        val vdsSeal = VdsSeal(header, messageGroup, signer)

        assertEquals("MELDEBESCHEINIGUNG", vdsSeal.documentType)
        assertEquals("Lorenzo", vdsSeal.getMessage("FIRST_NAME").toString())
        assertNull(vdsSeal.getMessage(0))

        val encodedSealBytes = vdsSeal.encoded
        println("Encoded seal bytes: ${encodedSealBytes.toHexString()}")

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

}