package de.tsenger.vdstools.idb

import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IcbJvmTest {
    @Test
    fun testParseAusweisersatz() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.SubstituteIdentityDocument) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)
        assertEquals(
            "AID<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8",
            icb.getMessage("MRZ_TD2")?.valueStr
        )
        assertEquals("2026-04-23", icb.getMessage("EXPIRY_DATE")?.valueStr)
        assertEquals(1, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
        assertEquals(996, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val digest = MessageDigest.getInstance("SHA-1")
        val signerIdentifier = digest.digest(cert.encoded).takeLast(5).toByteArray()
        println("signer identifier: ${signerIdentifier.toHexString()}")
        assertContentEquals(signerIdentifier, icb.payLoad.idbHeader.certificateReference)

        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testEmergencyTravelDocument() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.EmergencyTravelDocument) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)

        val etdBytes = icb.getMessage("EMERGENCY_TRAVEL_DOCUMENT")?.valueBytes
        assertNotNull(etdBytes)
        val mrz = VdsMessage.fromByteArray(etdBytes, "ICAO_EMERGENCY_TRAVEL_DOCUMENT").getFeature("MRZ")?.valueStr
        assertEquals(
            "PUD<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8", mrz
        )
        assertEquals("2026-04-23", icb.getMessage("EXPIRY_DATE")?.valueStr)
        assertEquals(2, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
        assertEquals(996, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testTemporaryPassport() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.TemporaryPassport) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(8)?.valueStr
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
        assertEquals("2027-01-31", icb.getMessage("EXPIRY_DATE")?.valueStr)
        assertEquals(6, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
        assertEquals(998, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testArrivalAttestation() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.ArrivalAttestation) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(0x81)?.valueStr
        assertEquals(
            "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.valueStr)
        assertEquals(0x0D, icb.getMessage(0x86)?.valueInt)
        assertEquals(996, icb.getMessage(0x80)?.valueBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testProvisionalResidenceDocument() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.ProvisionalResidenceDocument) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(0x81)?.valueStr
        assertEquals(
            "ABD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012", mrz
        )
        assertEquals("123456789", icb.getMessage(0x82)?.valueStr)
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.valueStr)
        assertEquals(0x0E, icb.getMessage(0x86)?.valueInt)


        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testCertifyingPermanentResidence() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.CertifyingPermanentResidence) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(3, numberOfMessages)

        assertEquals("123456789", icb.getMessage(0x82)?.valueStr)
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.valueStr)
        assertEquals(0x10, icb.getMessage(0x86)?.valueInt)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testFrontierWorkerPermit() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.FrontierWorkerPermit) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)

        assertEquals(996, icb.getMessage(0x80)?.valueBytes?.size)
        assertEquals(
            "AGD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012",
            icb.getMessage(0x81)?.valueStr
        )
        assertEquals("ABCDEFGHI", icb.getMessage(0x82)?.valueStr)
        assertEquals(0x11, icb.getMessage(0x86)?.valueInt)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testSupplementarySheetResidencePermit() {
        val icb = IcaoBarcode.fromString(IcbRawStringsJvm.SupplementarySheetResidencePermit) as IcaoBarcode
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
        assertEquals(4, numberOfMessages)


        assertEquals(
            "AZD<<5W1ETCGE25<<<<<<<<<<<<<<<\n" +
                    "8703123F2908258CHL<<<<<<<<<<<4\n" +
                    "BORIC<<BRYAN<<<<<<<<<<<<<<<<<<",
            icb.getMessage(0x07)?.valueStr
        )
        assertEquals("5W1ETCGE2", icb.getMessage(0x82)?.valueStr)
        assertEquals("ABCDEFGHI", icb.getMessage(0x85)?.valueStr)
        assertEquals(0x12, icb.getMessage(0x86)?.valueInt)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
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
    }
}