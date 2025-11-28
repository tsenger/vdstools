package de.tsenger.vdstools


import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import de.tsenger.vdstools.idb.*
import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.io.path.createParentDirectories

class BuildIdbTest {

    @Test
    fun testBuildVorlPA() {
        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val cert: X509Certificate = keystore.getCertificate("utts5b") as X509Certificate

        // 1. Build a IdbHeader
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            DataEncoder.buildCertificateReference(cert.encoded),
            "2027-05-01"
        )

        // 2. Build an ETD Message
        val mrz = "ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<D000000001D<<8308126<2707314<<<<<<<8"
        val vdsMessage = VdsMessage.Builder("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .addDocumentFeature("MRZ", mrz)
            .build()

        // 3. Build a MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage(0x02, vdsMessage.encoded)
            .addMessage(
                0x80,
                File("/home/tobi/git/VdsTools/src/commonTest/resources/testdaten/Erika Mustermann 100x129.jp2").readBytes()
            )
            .addMessage(0x84, "2027-07-31")
            .addMessage(0x86, 0x04)
            .build()

        // 4. Build a signed Icao Barcode
        val signature = signer.sign(header.encoded + messageGroup.encoded)
        val idbSignature = IdbSignature(signature)
        val payload = IdbPayload(header, messageGroup, null, idbSignature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "vorlPA.png")
        writeIcbDataFile(icb, "vorlPA")
    }

    @Test
    fun testBuildFiktion() {
        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val cert: X509Certificate = keystore.getCertificate("utts5b") as X509Certificate

        // 1. Build a IdbHeader
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            DataEncoder.buildCertificateReference(cert.encoded),
            "2027-05-01"
        )

        // 3. Build a MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage(0x81, "ABD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<F000000005UTO8308126<2804305T2705011")
            .addMessage(0x82, "X98723021")
            .addMessage(0x83, "960113000085")
            .addMessage(0x86, 0x0E)
            .build()

        // 4. Build a signed Icao Barcode
        val signature = signer.sign(header.encoded + messageGroup.encoded)
        val idbSignature = IdbSignature(signature)
        val payload = IdbPayload(header, messageGroup, null, idbSignature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "Fiktionsbescheinigung.png")
        writeIcbDataFile(icb, "Fiktionsbescheinigung")
    }

    @Test
    fun testBuildVorlPass() {
        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val cert: X509Certificate = keystore.getCertificate("utts5b") as X509Certificate

        // 1. Build a IdbHeader
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            DataEncoder.buildCertificateReference(cert.encoded),
            "2027-05-01"
        )

        // 3. Build a MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage(
                0x08,
                "PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<A000000000D<<8308126F2804305<<<<<<<<<<<<<<<2"
            )
            .addMessage(
                0x80,
                File("/home/tobi/git/VdsTools/src/commonTest/resources/testdaten/Erika Mustermann 100x129.jp2").readBytes()
            )
            .addMessage(0x84, "2028-04-30")
            .addMessage(0x86, 0x06)
            .build()

        // 4. Build a signed Icao Barcode
        val signature = signer.sign(header.encoded + messageGroup.encoded)
        val idbSignature = IdbSignature(signature)
        val payload = IdbPayload(header, messageGroup, null, idbSignature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "vorlPass.png")
        writeIcbDataFile(icb, "vorlPass")
    }

    fun generateDmBarcode(rawString: String, filename: String) {
        val dmw = DataMatrixWriter()
        val bitMatrix = dmw.encode(rawString, BarcodeFormat.DATA_MATRIX, 450, 450)

        // Define your own export Path and uncomment if needed
        val path = Path.of("generated_barcodes_bka/$filename").createParentDirectories()
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeIcbDataFile(icb: IcaoBarcode, filename: String) {
        val payloadFile = File("generated_barcodes_bka/${filename}_payload.txt")
        payloadFile.writeText(icb.payLoad.encoded.toHexString())
        val base32File = File("generated_barcodes_bka/${filename}_base32.txt")
        base32File.writeText(icb.rawString)
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
