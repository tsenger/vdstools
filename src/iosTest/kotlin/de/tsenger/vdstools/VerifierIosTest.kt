package de.tsenger.vdstools


import de.tsenger.vdstools.vds.DigitalSeal
import de.tsenger.vdstools.vds.VdsRawBytesIos
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSBundle
import platform.Security.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


class VerifierIosTest {

    @Ignore
    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    @Test
    fun testVerifyResidentPermit256BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit)
        val signerCertRef = digitalSeal?.signerCertRef
        assertEquals("UTTS5B", signerCertRef)

        val certPath = NSBundle.mainBundle.pathForResource("sealgen_UTTS5B", "crt")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)
        println(cert.subjectSummary)

        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.publicKeyDecoder(Curve("brainpoolP256r1"))
        val pubKeyBytes = getPublicKeyAsByteArray(cert.reference)
        println("PUBKEY Format : ${pubKeyBytes.toHexString()}")
        val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, pubKeyBytes)

        val verifier = Verifier(digitalSeal!!, ecPubKey)
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
    }

    @Ignore
    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    @Test
    fun testVerifyVisa224BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytesIos.visa_224bitSig)
        val signerCertRef = digitalSeal?.signerCertRef
        assertEquals("DETS32", signerCertRef)

        val certPath = NSBundle.mainBundle.pathForResource("sealgen_DETS32", "der")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)

        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.publicKeyDecoder(Curve("brainpoolP224r1"))
        val pubKeyBytes = getPublicKeyAsByteArray(cert.reference)
        println("PUBKEY Format : ${pubKeyBytes.toHexString()}")
        val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, pubKeyBytes)

        val verifier = Verifier(digitalSeal!!, ecPubKey)
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
    }


    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    fun loadCertificateFromFile(path: Path): Certificate {
        // Datei lesen mit Okio
        val fileData = FileSystem.SYSTEM.read(path) {
            readByteArray()
        }

        // CFData aus UByteArray erstellen
        val certData = CFDataCreate(null, fileData.toUByteArray().usePinned { it.addressOf(0) }, fileData.size.toLong())
            ?: throw IllegalArgumentException("Failed to create CFData from DER-encoded certificate")


        // Zertifikat erstellen
        val certificateRef = SecCertificateCreateWithData(null, certData)
            ?: throw IllegalArgumentException("Invalid X.509 certificate data")

        // Optional: Zertifikatsinformationen extrahieren
        val summary = SecCertificateCopySubjectSummary(certificateRef)?.toString() ?: "Unknown"

        return Certificate(certificateRef, summary)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun getEncodedCertificate(certificateRef: SecCertificateRef): ByteArray {
        // Zertifikatsdaten im DER-Format abrufen
        val certData = SecCertificateCopyData(certificateRef)
            ?: throw IllegalArgumentException("Failed to retrieve encoded certificate data")

        // CFDataRef in ByteArray umwandeln
        val length = CFDataGetLength(certData).toInt()
        val bytes = CFDataGetBytePtr(certData)?.readBytes(length)
            ?: throw IllegalArgumentException("Failed to convert certificate data to ByteArray")

        // Ressourcen freigeben
        CFRelease(certData)

        return bytes
    }


    @OptIn(ExperimentalForeignApi::class)
    fun getPublicKeyAsByteArray(certificateRef: SecCertificateRef): ByteArray {
        // Extrahiere den öffentlichen Schlüssel
        val publicKey = SecCertificateCopyKey(certificateRef)
            ?: throw IllegalArgumentException("Failed to extract public key from certificate")

        // Hole die externe Darstellung des Schlüssels
        val keyData = SecKeyCopyExternalRepresentation(publicKey, null)
            ?: throw IllegalArgumentException("Failed to get external representation of public key")

        // Konvertiere CFDataRef in ByteArray
        val length = CFDataGetLength(keyData).toInt()
        val bytes = CFDataGetBytePtr(keyData)?.readBytes(length)
            ?: throw IllegalArgumentException("Failed to read bytes from public key data")

        // Gib die Daten frei
        CFRelease(keyData)

        return bytes
    }

    data class Certificate @OptIn(ExperimentalForeignApi::class) constructor(
        val reference: SecCertificateRef,
        val subjectSummary: String
    )
}
