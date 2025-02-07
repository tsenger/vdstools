package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.KeyStore
import java.security.Security
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IdbBuilder {

    @Test
    fun build_Ausweisersatz() {
        val cert = keystore.getCertificate("utts5b")
        assertNotNull(cert)

        //Header
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            DataEncoder.buildCertificateReference(cert.encoded),
            "2025-02-07"
        )
        println("Header bytes: ${header.encoded.toHexString()}")

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
        val ecPrivKey = keystore.getKey("utts5b", keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val signature = IdbSignature(signer.sign(header.encoded + messageGroup.encoded))

        val payload = IdbPayload(header, messageGroup, null, signature)
        val icb = IcaoBarcode(isSigned = true, isZipped = false, barcodePayload = payload)

        println("Barcode ${icb.encoded}")

    }

    fun readBinaryFromResource(fileName: String): ByteArray {
        val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(fileName)
            ?: throw FileNotFoundException("File $fileName not found in resources!")
        return inputStream.use { it.readBytes() }
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