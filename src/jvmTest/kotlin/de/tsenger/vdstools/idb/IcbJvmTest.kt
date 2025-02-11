package de.tsenger.vdstools.idb

import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IcbJvmTest {
    @Test
    fun testParseAusweisersatz() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.SubstituteIdentityDocument)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
            val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
            assertEquals(4, numberOfMessages)
            assertEquals(
                "AID<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8",
                icb.getMessage("MRZ_TD2")?.valueStr
            )
            assertEquals("2026-04-23", icb.getMessage("DATE")?.valueStr)
            assertEquals(1, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
            assertEquals(996, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

            val cert = keystore.getCertificate("utts5b") as X509Certificate
            val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
            assertEquals(Verifier.Result.SignatureValid, verifier.verify())
        }
    }

    @Test
    fun testEmergencyTravelDocument() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.EmergencyTravelDocument)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
            val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
            assertEquals(4, numberOfMessages)

            val etdBytes = icb.getMessage("EMERGENCY_TRAVEL_DOCUMENT")?.valueBytes
            assertNotNull(etdBytes)
            val mrz = VdsMessage.fromByteArray(etdBytes, "ICAO_EMERGENCY_TRAVEL_DOCUMENT").getFeature("MRZ")?.valueStr
            assertEquals(
                "PUD<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8", mrz
            )
            assertEquals("2026-04-23", icb.getMessage("DATE")?.valueStr)
            assertEquals(2, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
            assertEquals(996, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

            val cert = keystore.getCertificate("utts5b") as X509Certificate
            val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
            assertEquals(Verifier.Result.SignatureValid, verifier.verify())
        }
    }

    @Test
    fun testTemporaryPassport() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.TemporaryPassport)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
            val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
            assertEquals(4, numberOfMessages)

            val mrz = icb.getMessage(8)?.valueStr
            assertEquals(
                "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
            )
            assertEquals("2027-01-31", icb.getMessage("DATE")?.valueStr)
            assertEquals(6, icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.valueInt)
            assertEquals(998, icb.getMessage("FACE_IMAGE")?.valueBytes?.size)

            val cert = keystore.getCertificate("utts5b") as X509Certificate
            val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
            assertEquals(Verifier.Result.SignatureValid, verifier.verify())
        }
    }

    @Test
    fun testArrivalAttestation() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.ArrivalAttestation)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
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
    }

    @Test
    fun testProvisionalResidenceDocument() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.ProvisionalResidenceDocument)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
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
    }

    @Test
    fun testCertifyingPermanentResidence() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.CertifyingPermanentResidence)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
            val numberOfMessages = icb.payLoad.idbMessageGroup.messagesList.size
            assertEquals(3, numberOfMessages)

            assertEquals("123456789", icb.getMessage(0x82)?.valueStr)
            assertEquals("ABC123456DEF", icb.getMessage(0x83)?.valueStr)
            assertEquals(0x10, icb.getMessage(0x86)?.valueInt)

            val cert = keystore.getCertificate("utts5b") as X509Certificate
            val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
            assertEquals(Verifier.Result.SignatureValid, verifier.verify())
        }
    }

    @Test
    fun testFrontierWorkerPermit() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.FrontierWorkerPermit)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
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
    }

    @Test
    fun testSupplementarySheetResidencePermit() {
        val icbResult = IcaoBarcode.fromString(IcbRawStrings.SupplementarySheetResidencePermit)
        assertNotNull(icbResult)
        icbResult.onSuccess { icb ->
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