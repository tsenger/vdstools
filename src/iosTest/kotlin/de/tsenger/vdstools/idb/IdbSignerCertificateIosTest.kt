package de.tsenger.vdstools.idb

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
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull


@OptIn(ExperimentalStdlibApi::class)
class IdbSignerCertificateIosTest {

    var encodedIdbSignerCertificate: ByteArray =
        ("7e8201bc308201b83082013ea00302010202015b300a06082a8648ce3d040302303e"
                + "310b30090603550406130255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b3009"
                + "060355040313025453301e170d3230303631303037313530305a170d3330303631303037313530305a303e310b3009060355040613"
                + "0255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b300906035504031302545330"
                + "5a301406072a8648ce3d020106092b24030302080101070342000408132a7243b3ccc29c271097081c96a729eefb8eb93630e53649"
                + "8e9b7ce1ced25d68a789d93bef39c04715c5ad3915d281c0754ecc08508bf66687efc630df88a32c302a30090603551d1304023000"
                + "301d0603551d0e04160414adc6bafc76d49aa2d92fface93d71033832c6e96300a06082a8648ce3d040302036800306502310087f8"
                + "5c8aa332659ed7ec30b8b61653353158f5ee6841c45c3b98fd1f14f0366203c934136c7444398f7fed359300203402307a95090526"
                + "35c0faceeb83b00ad56d345a48e9af9b7e27c1301b5c47c347a91e464223551174dfba9f85beda2350f452").hexToByteArray()


    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testConstructor() {
        val certPath = NSBundle.mainBundle.pathForResource("sealgen_UTTS5B", "crt")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)
        val idbSignerCertificate =
            IdbSignerCertificate(getEncodedCertificate(cert.reference))
        assertNotNull(idbSignerCertificate)
    }

    @Test
    fun testFromByteArray() {
        val signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate)
        assertNotNull(signCert)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testGetEncoded() {
        val certPath = NSBundle.mainBundle.pathForResource("sealgen_UTTS5B", "crt")?.toPath()
        val cert = loadCertificateFromFile(certPath!!)
        val idbSignerCertificate =
            IdbSignerCertificate(getEncodedCertificate(cert.reference))
        assertContentEquals(encodedIdbSignerCertificate, idbSignerCertificate.encoded)
    }

    @Test
    fun testGetX509Certificate() {
        val signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate)
        assertNotNull(signCert.certBytes)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun loadCertificateFromFile(path: Path): Certificate {
        // Datei lesen mit Okio
        val fileData = FileSystem.SYSTEM.read(path) {
            readByteArray()
        }

        // ByteArray in UByteArray konvertieren
        val ubyteData = fileData.toUByteArray()

        // CFData aus UByteArray erstellen
        val certData = ubyteData.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0), ubyteData.size.toLong())
        } ?: throw IllegalArgumentException("Could not create CFData from file data")

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

    data class Certificate @OptIn(ExperimentalForeignApi::class) constructor(
        val reference: SecCertificateRef,
        val subjectSummary: String
    )

}
