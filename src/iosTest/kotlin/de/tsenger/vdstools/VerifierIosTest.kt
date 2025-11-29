package de.tsenger.vdstools


import de.tsenger.vdstools.vds.DigitalSeal
import de.tsenger.vdstools.vds.VdsRawBytesIos
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
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateCopySubjectSummary
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecCertificateRef
import kotlin.test.Test
import kotlin.test.assertEquals


class VerifierIosTest {

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    @Test
    fun testVerifyResidentPermit256BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit) as DigitalSeal
        val signerCertRef = digitalSeal.signerCertRef
        assertEquals("UTTS5B", signerCertRef)

        val certPath = NSBundle.mainBundle.pathForResource("sealgen_UTTS5B", "crt")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)
        println(cert.subjectSummary)

        val pubKeyBytes = getPublicKeyAsByteArray(cert.reference)

        val verifier = Verifier(
            digitalSeal,
            pubKeyBytes,
            curveName = "brainpoolP256r1"
        )
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    @Test
    fun testVerifyVisa224BitSig() {
        val digitalSeal = DigitalSeal.fromByteArray(VdsRawBytesIos.visa_224bitSig) as DigitalSeal
        val signerCertRef = digitalSeal.signerCertRef
        assertEquals("DETS32", signerCertRef)

        val certPath = NSBundle.mainBundle.pathForResource("sealgen_DETS32", "crt")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)
        val pubKeyBytes = getPublicKeyAsByteArray(cert.reference)
        val verifier = Verifier(digitalSeal, pubKeyBytes, "brainpoolP224r1")
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
        // Get the DER-encoded certificate
        val certDer = getEncodedCertificate(certificateRef)

        // Extract SubjectPublicKeyInfo from the certificate
        // X.509 Certificate structure: SEQUENCE { tbsCertificate, signatureAlgorithm, signature }
        // TBSCertificate structure: SEQUENCE { version, serialNumber, signature, issuer, validity, subject, subjectPublicKeyInfo, ... }
        // We need to manually parse the DER structure to find the SubjectPublicKeyInfo

        return extractSubjectPublicKeyInfo(certDer)
    }

    /**
     * Extracts the SubjectPublicKeyInfo (public key in DER format) from an X.509 certificate
     */
    private fun extractSubjectPublicKeyInfo(certDer: ByteArray): ByteArray {
        var offset = 0

        // Parse outer SEQUENCE (Certificate)
        if (certDer[offset] != 0x30.toByte()) {
            throw IllegalArgumentException("Invalid certificate: expected SEQUENCE tag")
        }
        offset++

        // Skip length of outer SEQUENCE
        offset += getLengthByteCount(certDer[offset])

        // Parse inner SEQUENCE (TBSCertificate)
        if (certDer[offset] != 0x30.toByte()) {
            throw IllegalArgumentException("Invalid certificate: expected TBSCertificate SEQUENCE")
        }
        offset++

        // Skip length of TBSCertificate
        offset += getLengthByteCount(certDer[offset])

        // Now we need to skip through the TBSCertificate fields to reach SubjectPublicKeyInfo
        // Fields: version [0], serialNumber, signature, issuer, validity, subject, subjectPublicKeyInfo

        // Skip version (optional, context-specific [0])
        if (certDer[offset] == 0xA0.toByte()) {
            offset = skipDerElement(certDer, offset)
        }

        // Skip serialNumber (INTEGER)
        offset = skipDerElement(certDer, offset)

        // Skip signature algorithm (SEQUENCE)
        offset = skipDerElement(certDer, offset)

        // Skip issuer (SEQUENCE)
        offset = skipDerElement(certDer, offset)

        // Skip validity (SEQUENCE)
        offset = skipDerElement(certDer, offset)

        // Skip subject (SEQUENCE)
        offset = skipDerElement(certDer, offset)

        // Now we're at SubjectPublicKeyInfo (SEQUENCE)
        if (certDer[offset] != 0x30.toByte()) {
            throw IllegalArgumentException("Invalid certificate: expected SubjectPublicKeyInfo SEQUENCE at offset $offset")
        }

        val spkiLength = parseDerLength(certDer, offset + 1)
        val spkiLengthBytes = getLengthByteCount(certDer[offset + 1])
        val totalSpkiLength = 1 + spkiLengthBytes + spkiLength

        return certDer.copyOfRange(offset, offset + totalSpkiLength)
    }

    /**
     * Parses a DER length field
     */
    private fun parseDerLength(data: ByteArray, offset: Int): Int {
        val firstByte = data[offset].toInt() and 0xFF

        if (firstByte <= 127) {
            // Short form
            return firstByte
        } else {
            // Long form
            val numLengthBytes = firstByte - 128
            var length = 0
            for (i in 1..numLengthBytes) {
                length = (length shl 8) or (data[offset + i].toInt() and 0xFF)
            }
            return length
        }
    }

    /**
     * Returns the number of bytes used to encode the length
     */
    private fun getLengthByteCount(lengthByte: Byte): Int {
        val firstByte = lengthByte.toInt() and 0xFF
        return if (firstByte <= 127) 1 else 1 + (firstByte - 128)
    }

    /**
     * Skips over a DER element and returns the offset after the element
     */
    private fun skipDerElement(data: ByteArray, offset: Int): Int {
        // Skip tag
        var currentOffset = offset + 1

        // Parse and skip length
        val length = parseDerLength(data, currentOffset)
        currentOffset += getLengthByteCount(data[currentOffset])

        // Skip value
        currentOffset += length

        return currentOffset
    }

    data class Certificate @OptIn(ExperimentalForeignApi::class) constructor(
        val reference: SecCertificateRef,
        val subjectSummary: String
    )
}
