package de.tsenger.vdstools


import de.tsenger.vdstools.vds.DigitalSeal
import de.tsenger.vdstools.vds.VdsRawBytes
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.datetime.LocalDate
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.Security
import java.security.cert.X509Certificate
import java.text.ParseException
import java.util.*

class VerifierTest {
    @Test
    @Throws(ParseException::class, KeyStoreException::class, IOException::class)
    fun testVerifyArrivalAttestationDETS00027() {
        val digitalSeal: DigitalSeal = checkNotNull(DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestation))
        val signerCertRef: String = digitalSeal.signerCertRef
        assertEquals("DETS27", signerCertRef) // input validation

        val cert = keystore.getCertificate(signerCertRef.lowercase(Locale.getDefault())) as X509Certificate

        val signerIdentifier = getCCNString(cert)
        val serialNumber = cert.serialNumber.toInt()
        val x509SignerCertRef = String.format("%s%x", signerIdentifier, serialNumber)
        assertEquals(signerCertRef, x509SignerCertRef)

        val sigLocalDate = digitalSeal.sigDate

        assertEquals(LocalDate.parse("2020-01-13"), sigLocalDate)


//        try {
//            cert.checkValidity(sigLocalDate)
//        } catch (e: CertificateExpiredException) {
//            fail(e.message)
//        } catch (e: CertificateNotYetValidException) {
//            fail(e.message)
//        }

        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.publicKeyDecoder(Curve("brainpoolP256r1"))
        val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, cert.publicKey.encoded)

        val verifier = Verifier(digitalSeal, ecPubKey)
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
    }

    @Test
    @Throws(KeyStoreException::class, IOException::class)
    fun testVerifyResidentPermit256BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit)
        val signerCertRef = digitalSeal?.signerCertRef
        assertEquals("UTTS5B", signerCertRef)
        val cert = keystore.getCertificate(signerCertRef?.lowercase(Locale.getDefault())) as X509Certificate

        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.publicKeyDecoder(Curve("brainpoolP256r1"))
        println("PUBKEY Format : ${cert.publicKey.format}")
        val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, cert.publicKey.encoded)

        val verifier = Verifier(digitalSeal!!, ecPubKey)
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
    }

    @Test
    @Throws(KeyStoreException::class, IOException::class)
    fun testVerifyVisa224BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytes.visa_224bitSig)
        val signerCertRef = digitalSeal?.signerCertRef
        assertEquals("DETS32", signerCertRef)
        val cert = keystore.getCertificate(signerCertRef?.lowercase(Locale.getDefault())) as X509Certificate

        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.publicKeyDecoder(Curve("brainpoolP224r1"))
        println("PUBKEY Format : ${cert.publicKey.format}")
        val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, cert.publicKey.encoded)

        val verifier = Verifier(digitalSeal!!, ecPubKey)
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
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

        fun getCCNString(x509: X509Certificate): String {
            val x500name = X500Name(x509.subjectX500Principal.name)
            val c = x500name.getRDNs(BCStyle.C)[0]
            val cn = x500name.getRDNs(BCStyle.CN)[0]
            val cString = IETFUtils.valueToString(c.first.value)
            val cnString = IETFUtils.valueToString(cn.first.value)
            return cString + cnString
        }
    }
}
