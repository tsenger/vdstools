package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.junit.BeforeClass
import org.junit.Test
import vdstools.DataEncoder
import vdstools.Signer
import vdstools.vds.DigitalSeal
import vdstools.vds.VdsHeader
import vdstools.vds.VdsMessage
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey

class DataMatrixTest {
    var keyStorePassword: String = "vdstools"
    var keyStoreFile: String = "src/test/resources/vdstools_testcerts.bks"

    @Test
    @Throws(
        IOException::class,
        KeyStoreException::class,
        UnrecoverableKeyException::class,
        NoSuchAlgorithmException::class
    )
    fun testSaveDataMatrixToFile() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessage.Builder("RESIDENCE_PERMIT").addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("PASSPORT_NUMBER", passportNumber).build()

        val ks = keystore
        val ecKey = ks!!.getKey("utts5b", keyStorePassword.toCharArray()) as ECPrivateKey
        val signer = Signer(ecKey)
        val cert = ks.getCertificate("utts5b") as X509Certificate

        val vdsHeader = VdsHeader.Builder(vdsMessage.vdsType)
            .setIssuingCountry("D  ")
            .setSignerIdentifier("UTTS")
            .setCertificateReference("5B")
            .build()
        println("Header: " + Hex.toHexString(vdsHeader.encoded))
        val digitalSeal = DigitalSeal(vdsHeader, vdsMessage, signer)

        val dmw = DataMatrixWriter()
        val bitMatrix = dmw.encode(
            DataEncoder.encodeBase256(digitalSeal.encoded), BarcodeFormat.DATA_MATRIX,
            450, 450
        )

        // Define your own export Path and uncomment if needed
        val path = Path.of("test/test.png")
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    @Test
    @Throws(IOException::class)
    fun testDmFromRawString() {
        val rawString =
            "Ü\u0003j¼m4\u008A(4\u0016OÕ\u0096OÕ\u0096]\u0001\u0002,ÝR\u0013SÙ¢us[Ô\u0013KÙu·t\u0013<\u0013<\u0013<\u0013<\u0019¥\u0019¥\u0019¥\u001Er°Á\u001B\u000EL|&uKýþ1\u0004\u0003 \u0001 \u0005\u0006Ï7\u0019¦'\u008Dÿ8FÈ `B2·|\u008Aå4jóà&Ì\u0093à,ã\u001E\u0084\u0092\u001BTe¤Ô®Àh\u009D\u0001Õ\u008C\u0011ì\u0093Ü\"\u001E\u0002)ìo3\u001C\u009A\u0090\u008A\u00ADBÈiM#"
        val dmw = DataMatrixWriter()
        val bitMatrix = dmw.encode(
            rawString, BarcodeFormat.DATA_MATRIX,
            450, 450
        )
        val path = Path.of("test/test.png")
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    private val keystore: KeyStore?
        get() {
            val keystore: KeyStore

            try {
                keystore = KeyStore.getInstance("BKS", "BC")
                val fis = FileInputStream(keyStoreFile)
                keystore.load(fis, keyStorePassword.toCharArray())
                fis.close()
                return keystore
            } catch (e: KeyStoreException) {
                Logger.w("Error while opening keystore '" + keyStoreFile + "': " + e.message)
                return null
            } catch (e: NoSuchProviderException) {
                Logger.w("Error while opening keystore '" + keyStoreFile + "': " + e.message)
                return null
            } catch (e: NoSuchAlgorithmException) {
                Logger.w("Error while opening keystore '" + keyStoreFile + "': " + e.message)
                return null
            } catch (e: CertificateException) {
                Logger.w("Error while opening keystore '" + keyStoreFile + "': " + e.message)
                return null
            } catch (e: IOException) {
                Logger.w("Error while opening keystore '" + keyStoreFile + "': " + e.message)
                return null
            }
        }

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadBC() {
            Security.addProvider(BouncyCastleProvider())
        }
    }
}
