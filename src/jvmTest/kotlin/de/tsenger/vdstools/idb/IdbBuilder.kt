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

    @Test
    fun parser_Ausweisersatz() {
        val barcodeData =
            "NDB1BNK6ADJL2PECXOAA7TXMWDAQEEOAIEA7EAAAAADDKKAQCADIKQ4FAAAAACRTHI6LQNJYDEIAAAAAAA2TQGIQAAAAAI5VHAMTIAAAAAFTJNBSHEAAAACAQAAAAMQAACBYHAAAAAAAAB5RW63DSAEAAAAAAAAIQAAAADJZGK4ZAAAAAAETSMVZWGAJMAD7ACLAA7YCAIAAAAAAGU4BSMP7U772RAAUQAAAAAAAGIAAAACAQAAAAAAAAAAAAAAAAAZAAAAAICAAAAAAAAAAAAAAACBYBAH7VYAAXIJTTKZ2MM5GGOZCGGZDDMRVMHYED4CB5UP7VEAAMAAAAAAIAAMCAIAAA75SAADQAAFGFIX2KKAZF6MRSG37ZAAAKAAAAAAADB4AAD74TY7FX6HL6CBIU4OROHXUXWYFSZTVXFI47EE2NADZRJWKVIGHF7BZDEJUHKDBB2LVVJBUG6MCWD66UJDQPTNHAIKTKEB4THMTRBKM6ORXIEW5WWVQDEYMMIFHA43M5OEHWK62OQHQQKTBLBNONJTM3INJTFMRPXM6NUTBYIQWXPHK6EMENBL25ZRIW5FXG2PZO3CLJC6WCXCLFGNZKYPSKOQ7EULA7BVUAKBQ44Q6HCLT5RDUZM4D3TT55GA7H57NQ7G7LXSG4W4NNAT344KM5LE7EMSDFOE5OFQDYF6PQYZRXR3RQSBCDGV34YNJG3VUWGUJ3DL7TJAYWW7YVI5GVGPX4IKM25DFVEAGB6OM2VFHQAGMFNJFT56I7V5XIRMFOIFJDG2SRS5GFCKY6UUYUVPBL3TG2ULE6ULYNIICKTLUJK6ALUA2SNNU7TSPBXVQVRPEJ7R7UHJWBWI6XGNKWRRBXEFB27VLV3OEVKMVQBCLRFUWXFYXFVOWMF7I763YAUQXDNTP7E42DG26XKBB4HYG4UPR3NQONHCMQKS5FOZOP6JDW75G5EPKRLSGBURBIMMLAW72X4TTXLFH5ATDGB4VCA6US4G57CFEPCE6SB6JUZJGDLWTD2L7YLDXCTYPXZYSMPITJV5ABIDRACHAYDBJZXQXWIPCWWX6TVPXTIA6MQQJNAVSETK7IYNK6NXA66V2A6UELOCCMQDDEKQYNOCLDZ2NGWSIRQFODRERJXLTKFZ2PIUHF34VJDGYKAAFN67I2WUVWD2UY3FOBWKVU5YXMYV5FRRN6DWNJWA76JYR2ONQ72CZHBHYBTN7XNRGVNVRMMT7DZLUXZPOA2HA46H6ADOTMVJTNWAG6SENPRKH4SJQZWGSFXXFGP4P6Q3J5NSRFZZBLFGHVFGLQIYB5VGSHBBE2YEXIPMP4XGND45NCNSKOJIN6LIT4ZQO2QXRWLOX6BY7QNMEHHI4A5VUX54LSUSASKQRNZUREAU2IATUK5OYDB3XGQTZBHZC3SHGSQJPCRSBJVFG72WXX6RN2R34JFPYXVPUKMEM55ZTGXLR76AJR2TPT7HKGO6RTEIDE6RBFNRKJRR4NPILHJ5XLUEMZKB4A65NSF6T2YN3Y3T4EXDIQCQ3XQ2N7ERQQKZYWTTVPVCNFAJMMNE4JXNFCY4FRH27KA5P3LWZGCF4TIPXSWR2QZESM4AOSH74XRORBXWWCTS3VO4CW6Q773GATAWYCCNI3D3VYFSGUIFTVVMJILDAV6PCAU2V4CM6CS3WPO63Z2NFMCSVBVFY77MTRKMXYKG6BGPATIWCAIACASNNIMAIBP5ACT5TQP4GH7YMVOQQDPCL3X4X3F6CSH5FRS3WWDEZV3K67KD655RRGJSTKASBXEML6KLJDGQRBAYONLFJRVETDODJXIKUFELQSSSY6S4"
        val icbResult = IcaoBarcode.fromString(barcodeData)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
            val messageList = icb.payLoad.idbMessageGroup.getMessagesList()
            for (message in messageList) {
                println("${message.messageTypeName}: ${message.valueStr}")
            }
            //icb.payLoad.idbMessageGroup.g
        }
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