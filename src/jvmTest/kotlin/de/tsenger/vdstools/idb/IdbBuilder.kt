package de.tsenger.vdstools.idb

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
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
        val faceImage = IdbMessage(0x80, readBinaryFromResource("face_image_gen.jp2"))
        val mrzTd2 = IdbMessage(
            0x81, DataEncoder.encodeC40(
                "AID<<KOEPPENIK<<JONATHAN<GERALD<<<<<" +
                        "2L1T3QPB04D<<8506210M2604239<<<<<<<8"
            )
        )
        val expiryDate = IdbMessage(0x84, DataEncoder.encodeMaskedDate("2026-04-23"))
        val docIdentifier = IdbMessage(0x86, byteArrayOf(0x01))

        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(faceImage)
        messageGroup.addMessage(mrzTd2)
        messageGroup.addMessage(expiryDate)
        messageGroup.addMessage(docIdentifier)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "SubstituteIdentityDocument.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_ReiseausweisAlsPassersatz() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrz = "PUD<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8"
        val vdsMessage = VdsMessage.Builder("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .addDocumentFeature("MRZ", mrz)
            .build()
        val etd = IdbMessage(0x02, vdsMessage.encoded)
        println(etd.encoded.toHexString())
        val faceImage = IdbMessage(0x80, readBinaryFromResource("face_image_gen.jp2"))

        val expiryDate = IdbMessage(0x84, DataEncoder.encodeMaskedDate("2026-04-23"))
        val docIdentifier = IdbMessage(0x86, byteArrayOf(0x02))

        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(etd)
        messageGroup.addMessage(faceImage)
        messageGroup.addMessage(expiryDate)
        messageGroup.addMessage(docIdentifier)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "EmergencyTravelDocument.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_TemporaryPassport() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04"
        val mrz = IdbMessage(0x08, DataEncoder.encodeC40(mrzString))
        val faceImage = IdbMessage(0x80, readBinaryFromResource("face_image_gen_female.jp2"))

        val expiryDate = IdbMessage(0x84, DataEncoder.encodeMaskedDate("2027-01-31"))
        val docIdentifier = IdbMessage(0x86, byteArrayOf(0x06))

        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(mrz)
        messageGroup.addMessage(faceImage)
        messageGroup.addMessage(expiryDate)
        messageGroup.addMessage(docIdentifier)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "TemporaryPassport.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_ArrivalAttestation() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"

        val faceImage = IdbMessage(0x80, readBinaryFromResource("face_image_gen.jp2"))
        val mrz = IdbMessage(0x81, DataEncoder.encodeC40(mrzString))
        val azr = IdbMessage(0x83, DataEncoder.encodeC40("ABC123456DEF"))
        val docIdentifier = IdbMessage(0x86, byteArrayOf(0x0D))

        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(mrz)
        messageGroup.addMessage(faceImage)
        messageGroup.addMessage(azr)
        messageGroup.addMessage(docIdentifier)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "ArrivalAttestation.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_ProvisionalResidenceDocument() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val mrzString = "ABD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012"

        val mrz = IdbMessage(0x81, DataEncoder.encodeC40(mrzString))
        val docNumber = IdbMessage(0x82, DataEncoder.encodeC40("123456789"))
        val azr = IdbMessage(0x83, DataEncoder.encodeC40("ABC123456DEF"))
        val docIdentifier = IdbMessage(0x86, byteArrayOf(0x0E))

        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(mrz)
        messageGroup.addMessage(docNumber)
        messageGroup.addMessage(azr)
        messageGroup.addMessage(docIdentifier)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "ProvisionalResidenceDocument.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_CertifyingPermanentResidence() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = getHeader()

        //MessageGroup
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(0x82, "123456789")
        messageGroup.addMessage(0x83, "ABC123456DEF")
        messageGroup.addMessage(0x86, 0x10)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "CertifyingPermanentResidence.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_FrontierWorkerPermit() {

        val header = getHeader()
        //MessageGroup
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(0x80, readBinaryFromResource("face_image_gen.jp2"))
        messageGroup.addMessage(0x81, "AGD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012")
        messageGroup.addMessage(0x82, "ABCDEFGHI")
        messageGroup.addMessage(0x86, 0x11)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "FrontierWorkerPermit.png")
        println("Barcode ${icb.encoded}")
    }

    @Test
    fun build_SupplementarySheetResidencePermit() {

        val header = getHeader()
        //MessageGroup
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(
            0x07,
            "AZD<<5W1ETCGE25<<<<<<<<<<<<<<<\n" +
                    "8703123F2908258CHL<<<<<<<<<<<4\n" +
                    "BORIC<<BRYAN<<<<<<<<<<<<<<<<<<"
        )
        messageGroup.addMessage(0x82, "5W1ETCGE2")
        messageGroup.addMessage(0x85, "ABCDEFGHI")
        messageGroup.addMessage(0x86, 0x12)

        println("MessageGroupBytes: ${messageGroup.encoded.toHexString()}")

        // Signature
        val signature = buildSignature(header.encoded + messageGroup.encoded)

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        generateDmBarcode(icb.encoded, "SupplementarySheetResidencePermit.png")
        println("Barcode ${icb.encoded}")
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