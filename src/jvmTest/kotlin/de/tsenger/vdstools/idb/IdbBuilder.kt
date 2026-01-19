package de.tsenger.vdstools.idb

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.vds.VdsMessageGroup
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path
import java.security.KeyStore
import java.security.Security
import kotlin.io.path.createParentDirectories
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IdbBuilder {

    @Test
    fun build_SubstituteIdentityDocument() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x80, readBinaryFromResource("face_image_gen.jp2"))
            .addFeature(0x81, "AID<<KOEPPENIK<<JONATHAN<GERALD<<<<<2L1T3QPB04D<<8506210M2604239<<<<<<<8")
            .addFeature(0x84, "2026-04-23")
            .addFeature(0x86, 0x01)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "SubstituteIdentityDocument.png")
        writeIcbDataFile(icb, "SubstituteIdentityDocument")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_ReiseausweisAlsPassersatz() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrz = "PUD<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8"
        val vdsMessage = VdsMessageGroup.Builder("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .addDocumentFeature("MRZ", mrz)
            .build()

        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x02, vdsMessage.encoded)
            .addFeature(0x80, readBinaryFromResource("face_image_gen.jp2"))
            .addFeature(0x84, "2026-04-23")
            .addFeature(0x86, 0x02)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "EmergencyTravelDocument.png")
        writeIcbDataFile(icb, "EmergencyTravelDocument")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_TemporaryPassport() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04"

        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x08, mrzString)
            .addFeature(0x80, readBinaryFromResource("face_image_gen_female.jp2"))
            .addFeature(0x84, "2027-01-31")
            .addFeature(0x86, 0x06)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "TemporaryPassport.png")
        writeIcbDataFile(icb, "TemporaryPassport")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_ArrivalAttestation() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"

        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x81, mrzString)
            .addFeature(0x80, readBinaryFromResource("face_image_gen.jp2"))
            .addFeature(0x83, "ABC123456DEF")
            .addFeature(0x86, 0x0D)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "ArrivalAttestation.png")
        writeIcbDataFile(icb, "ArrivalAttestation")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_ProvisionalResidenceDocument() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "ABD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012"

        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x81, mrzString)
            .addFeature(0x82, "123456789")
            .addFeature(0x83, "ABC123456DEF")
            .addFeature(0x86, 0x0E)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "ProvisionalResidenceDocument.png")
        writeIcbDataFile(icb, "ProvisionalResidenceDocument")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_CertifyingPermanentResidence() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x82, "123456789")
            .addFeature(0x83, "ABC123456DEF")
            .addFeature(0x86, 0x10)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "CertifyingPermanentResidence.png")
        writeIcbDataFile(icb, "CertifyingPermanentResidence")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_FrontierWorkerPermit() {

        val header = getHeader()
        //MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(0x80, readBinaryFromResource("face_image_gen.jp2"))
            .addFeature(0x81, "AGD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012")
            .addFeature(0x82, "ABCDEFGHI")
            .addFeature(0x86, 0x11)
            .build()


        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "FrontierWorkerPermit.png")
        writeIcbDataFile(icb, "FrontierWorkerPermit")
        println("Barcode ${icb.rawString}")
    }

    @Test
    fun build_SupplementarySheetResidencePermit() {

        val header = getHeader()
        //MessageGroup
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature(
                0x07,
                "AZD<<5W1ETCGE25<<<<<<<<<<<<<<<\n" +
                        "8703123F2908258CHL<<<<<<<<<<<4\n" +
                        "BORIC<<BRYAN<<<<<<<<<<<<<<<<<<"
            )
            .addFeature(0x82, "5W1ETCGE2")
            .addFeature(0x85, "ABCDEFGHI")
            .addFeature(0x86, 0x12)
            .build()

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.rawString, "SupplementarySheetResidencePermit.png")
        writeIcbDataFile(icb, "SupplementarySheetResidencePermit")
        println("Barcode ${icb.rawString}")
    }

    fun buildSignature(bytesToSign: ByteArray): IdbSignature {
        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        return IdbSignature(signer.sign(bytesToSign))
    }


    fun generateDmBarcode(rawString: String, filename: String) {
        val dmw = DataMatrixWriter()
        val bitMatrix = dmw.encode(rawString, BarcodeFormat.DATA_MATRIX, 450, 450)

        // Define your own export Path and uncomment if needed
        val path = Path.of("generated_barcodes/$filename").createParentDirectories()
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    fun writeIcbDataFile(icb: IdbSeal, filename: String) {
        val payloadFile = File("generated_barcodes/${filename}_payload.txt")
        payloadFile.writeText(icb.payLoad.encoded.toHexString())
        val base32File = File("generated_barcodes/${filename}_base32.txt")
        base32File.writeText(icb.rawString)
    }

    fun readBinaryFromResource(fileName: String): ByteArray {
        val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(fileName)
            ?: throw FileNotFoundException("File $fileName not found in resources!")
        return inputStream.use { it.readBytes() }
    }

    fun getHeader(): IdbHeader {
        val cert = keystore.getCertificate("utts5b")
        return IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            DataEncoder.buildCertificateReference(cert.encoded),
            "2025-02-11"
        )
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