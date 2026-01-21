package de.tsenger.vdstools.idb

import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.vds.MessageValue
import de.tsenger.vdstools.vds.VdsMessageGroup
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
        val icb = IdbSeal.fromString(IcbRawStringsJvm.SubstituteIdentityDocument) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)
        assertEquals(
            "AID<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8",
            icb.getMessage("MRZ_TD2")?.value.toString()
        )
        assertEquals("2026-04-23", icb.getMessage("EXPIRY_DATE")?.value.toString())
        assertEquals(1, (icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.value as? MessageValue.ByteValue)?.value)
        assertEquals(996, icb.getMessage("FACE_IMAGE")?.value?.rawBytes?.size)

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
        val icb = IdbSeal.fromString(IcbRawStringsJvm.EmergencyTravelDocument) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)

        val etdBytes = icb.getMessage("EMERGENCY_TRAVEL_DOCUMENT")?.value?.rawBytes
        assertNotNull(etdBytes)
        val mrz = VdsMessageGroup.fromByteArray(etdBytes, "ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .getMessage("MRZ")?.value.toString()
        assertEquals(
            "PUD<<KOEPPENIK<<JONATHAN<GERALD<<<<<\n2L1T3QPB04D<<8506210M2604239<<<<<<<8", mrz
        )
        assertEquals("2026-04-23", icb.getMessage("EXPIRY_DATE")?.value.toString())
        assertEquals(2, (icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.value as? MessageValue.ByteValue)?.value)
        assertEquals(996, icb.getMessage("FACE_IMAGE")?.value?.rawBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testTemporaryPassport() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.TemporaryPassport) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(8)?.value.toString()
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
        assertEquals("2027-01-31", icb.getMessage("EXPIRY_DATE")?.value.toString())
        assertEquals(6, (icb.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.value as? MessageValue.ByteValue)?.value)
        assertEquals(998, icb.getMessage("FACE_IMAGE")?.value?.rawBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testArrivalAttestation() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.ArrivalAttestation) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(0x81)?.value.toString()
        assertEquals(
            "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.value.toString())
        assertEquals(0x0D, (icb.getMessage(0x86)?.value as? MessageValue.ByteValue)?.value)
        assertEquals(996, icb.getMessage(0x80)?.value?.rawBytes?.size)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testProvisionalResidenceDocument() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.ProvisionalResidenceDocument) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)

        val mrz = icb.getMessage(0x81)?.value.toString()
        assertEquals(
            "ABD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012", mrz
        )
        assertEquals("123456789", icb.getMessage(0x82)?.value.toString())
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.value.toString())
        assertEquals(0x0E, (icb.getMessage(0x86)?.value as? MessageValue.ByteValue)?.value)


        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testCertifyingPermanentResidence() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.CertifyingPermanentResidence) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(3, numberOfMessages)

        assertEquals("123456789", icb.getMessage(0x82)?.value.toString())
        assertEquals("ABC123456DEF", icb.getMessage(0x83)?.value.toString())
        assertEquals(0x10, (icb.getMessage(0x86)?.value as? MessageValue.ByteValue)?.value)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testFrontierWorkerPermit() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.FrontierWorkerPermit) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)

        assertEquals(996, icb.getMessage(0x80)?.value?.rawBytes?.size)
        assertEquals(
            "AGD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018T2506012",
            icb.getMessage(0x81)?.value.toString()
        )
        assertEquals("ABCDEFGHI", icb.getMessage(0x82)?.value.toString())
        assertEquals(0x11, (icb.getMessage(0x86)?.value as? MessageValue.ByteValue)?.value)

        val cert = keystore.getCertificate("utts5b") as X509Certificate
        val verifier = Verifier(icb, cert.publicKey.encoded, "brainpoolP256r1")
        assertEquals(Verifier.Result.SignatureValid, verifier.verify())

    }

    @Test
    fun testSupplementarySheetResidencePermit() {
        val icb = IdbSeal.fromString(IcbRawStringsJvm.SupplementarySheetResidencePermit) as IdbSeal
        assertNotNull(icb)

        val numberOfMessages = icb.payLoad.idbMessageGroup.messageList.size
        assertEquals(4, numberOfMessages)


        assertEquals(
            "AZD<<5W1ETCGE25<<<<<<<<<<<<<<<\n" +
                    "8703123F2908258CHL<<<<<<<<<<<4\n" +
                    "BORIC<<BRYAN<<<<<<<<<<<<<<<<<<",
            icb.getMessage(0x07)?.value.toString()
        )
        assertEquals("5W1ETCGE2", icb.getMessage(0x82)?.value.toString())
        assertEquals("ABCDEFGHI", icb.getMessage(0x85)?.value.toString())
        assertEquals(0x12, (icb.getMessage(0x86)?.value as? MessageValue.ByteValue)?.value)

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