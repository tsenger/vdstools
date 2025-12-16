package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.SignerJvmTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.Security
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class DigitalSealJvmTest {
    @Test
    @Throws(IOException::class)
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.socialInsurance) as DigitalSeal
        Assert.assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        Assert.assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.valueStr)
        Assert.assertEquals("Perschweiß", seal.getMessage("SURNAME")?.valueStr)
        Assert.assertEquals("Oscar", seal.getMessage("FIRST_NAME")?.valueStr)
        Assert.assertEquals("Jâcobénidicturius", seal.getMessage("BIRTH_NAME")?.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.arrivalAttestationV02) as DigitalSeal
        Assert.assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        Assert.assertEquals("0004F", seal.certificateReference)
        Assert.assertEquals("ABC123456DEF", seal.getMessage("AZR")!!.valueStr)
        Assert.assertNull(seal.getMessage("FIRST_NAME"))
    }

    @Test
    @Throws(IOException::class)
    fun testParseResidentPermit() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.residentPermit) as DigitalSeal
        Assert.assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        Assert.assertEquals("UFO001979", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.supplementSheet) as DigitalSeal
        Assert.assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        Assert.assertEquals("PA0000005", seal.getMessage("SHEET_NUMBER")?.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.emergenyTravelDoc) as DigitalSeal
        Assert.assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
    }

    @Test
    @Throws(IOException::class)
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.addressStickerId) as DigitalSeal
        Assert.assertEquals("T2000AK47", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        Assert.assertEquals("05314000", seal.getMessage("AGS")?.valueStr)
        Assert.assertEquals("53175HEINEMANNSTR11", seal.getMessage("ADDRESS")?.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.addressStickerPassport) as DigitalSeal
        Assert.assertEquals("PA5500K11", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        Assert.assertEquals("03359010", seal.getMessage("AGS")?.valueStr)
        Assert.assertEquals("21614", seal.getMessage("POSTAL_CODE")?.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.visa_224bitSig) as DigitalSeal
        Assert.assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<", seal.getMessage(
                "MRZ_MRVB"
            )?.valueStr
        )
        Assert.assertEquals("47110815P", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
        Assert.assertEquals(
            "a00000", Hex.toHexString(
                seal.getMessage("DURATION_OF_STAY")?.valueBytes
            )
        )
        Assert.assertNull(seal.getMessage("NUMBER_OF_ENTRIES"))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.residentPermit) as DigitalSeal
        Assert.assertTrue(Arrays.areEqual(VdsRawBytesJvm.residentPermit, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.arrivalAttestation) as DigitalSeal
        Assert.assertTrue(Arrays.areEqual(VdsRawBytesJvm.arrivalAttestation, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.arrivalAttestationV02) as DigitalSeal
        Assert.assertTrue(Arrays.areEqual(VdsRawBytesJvm.arrivalAttestationV02, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_fc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.fictionCert) as DigitalSeal
        Assert.assertTrue(Arrays.areEqual(VdsRawBytesJvm.fictionCert, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.arrivalAttestationV02) as DigitalSeal
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        Assert.assertEquals(rawString, seal2.rawString)
        Assert.assertEquals(
            Hex.toHexString(VdsRawBytesJvm.arrivalAttestationV02), Hex.toHexString(
                seal2.encoded
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun testgetRawString2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesJvm.tempPerso) as DigitalSeal
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        Assert.assertEquals(rawString, seal2.rawString)
        Assert.assertEquals(
            Hex.toHexString(VdsRawBytesJvm.tempPerso), Hex.toHexString(
                seal2.encoded
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    @Test
    @Throws(IOException::class, KeyStoreException::class)
    fun testBuildDigitalSeal() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessage.Builder("RESIDENCE_PERMIT")
            .addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("PASSPORT_NUMBER", passportNumber)
            .build()

        val ecPrivKey = SignerJvmTest.keystore.getKey(
            "dets32",
            SignerJvmTest.keyStorePassword.toCharArray()
        ) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP224r1")

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder(vdsMessage.vdsType)
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()
        val digitalSeal = DigitalSeal(vdsHeader, vdsMessage, signer)
        Assert.assertNotNull(digitalSeal)
        val expectedHeaderMessage = Arrays.concatenate(
            Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
            Hex.decode(
                "fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306"
            )
        )
        val headerMessage = Arrays.copyOfRange(digitalSeal.encoded, 0, 76)
        // System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
        Assert.assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage))
    }

    @Test
    @Throws(IOException::class)
    fun testBuildDigitalSeal2() {
        val ecPrivKey = SignerJvmTest.keystore.getKey(
            "dets32",
            SignerJvmTest.keyStorePassword.toCharArray()
        ) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP224r1")
        val header = VdsHeader.Builder("ARRIVAL_ATTESTATION")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"
        val azr = "ABC123456DEF"
        val vdsMessage = VdsMessage.Builder(header.vdsType)
            .addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("AZR", azr)
            .build()
        val digitalSeal = DigitalSeal(header, vdsMessage, signer)

        Assert.assertNotNull(digitalSeal)
        val expectedHeaderMessage = Hex.decode(
            "dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24"
        )
        val headerMessage = Arrays.copyOfRange(digitalSeal.encoded, 0, 78)
        Assert.assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage))
    }

    @Test
    fun testUnknownSealType() {
        val rawBytes = VdsRawBytesJvm.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = DigitalSeal.fromByteArray(rawBytes) as DigitalSeal
        Assert.assertNotNull(seal)
        Assert.assertNotNull(seal.documentType)
    }

    companion object {
        //@formatter:off
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/commonTest/resources/vdstools_testcerts.bks"
        var keystore: KeyStore? = null
        
        @JvmStatic
        @BeforeClass
        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore?.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
 }}
