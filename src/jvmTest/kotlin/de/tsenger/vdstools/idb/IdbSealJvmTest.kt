package de.tsenger.vdstools.idb


import de.tsenger.vdstools.DataEncoder.buildCertificateReference
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.vds.VdsMessageGroup
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate

@OptIn(ExperimentalStdlibApi::class)
class IdbSealJvmTest {

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

    @Test
    fun testBuildIdbSeal() {

        val signerCertRef = "utts5b"

        // Build Header
        val cert = keystore.getCertificate(signerCertRef) as X509Certificate
        val certRef = buildCertificateReference(cert.encoded)
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            certRef,
            "2025-01-31"
        )

        // Build Emergency Travel Document VdsMessageGroup
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val vdsMessage = VdsMessageGroup.Builder("EMERGENCY_TRAVEL_DOCUMENT")
            .addMessage("MRZ", mrz)
            .build()

        // Add ETD to an IdbMessageGroup
        val messageGroup = IdbMessageGroup.Builder().addMessage("EMERGENCY_TRAVEL_DOCUMENT", vdsMessage.encoded).build()

        // Generate Signature
        val ecPrivKey = keystore.getKey(signerCertRef, keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val signature = IdbSignature(signer.sign(header.encoded + messageGroup.encoded))

        val icb = IdbSeal('B', IdbPayload(header, messageGroup, null, signature))
        assertTrue(
            icb.rawString.startsWith("RDB1BNK6ADJL2PECXOAAUAUMWCNACGIBDAXF2CNMHLF3OYBTNIF5VT2GGVPATHQJTYEZ4CM6D73Z2FE4O4Q7RLE6RVZJNXMTHKH7G")
        )
    }
}
