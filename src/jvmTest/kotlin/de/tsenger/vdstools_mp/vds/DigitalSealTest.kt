package de.tsenger.vdstools_mp.vds

import co.touchlab.kermit.Logger
import de.tsenger.vdstools_mp.DataEncoder
import de.tsenger.vdstools_mp.Signer
import de.tsenger.vdstools_mp.SignerTest
import kotlinx.datetime.Clock
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


class DigitalSealTest {
    @Test
    @Throws(IOException::class)
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.socialInsurance)
        Assert.assertEquals("SOCIAL_INSURANCE_CARD", seal!!.vdsType)
        Assert.assertEquals("65170839J003", seal.getFeature("SOCIAL_INSURANCE_NUMBER")!!.valueStr)
        Assert.assertEquals("Perschweiß", seal.getFeature("SURNAME")!!.valueStr)
        Assert.assertEquals("Oscar", seal.getFeature("FIRST_NAME")!!.valueStr)
        Assert.assertEquals("Jâcobénidicturius", seal.getFeature("BIRTH_NAME")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02)
        Assert.assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        Assert.assertEquals("0004F", seal.certificateReference)
        Assert.assertEquals("ABC123456DEF", seal.getFeature("AZR")!!.valueStr)
        Assert.assertNull(seal.getFeature("FIRST_NAME"))
    }

    @Test
    @Throws(IOException::class)
    fun testParseResidentPermit() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit)
        Assert.assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        Assert.assertEquals("UFO001979", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.supplementSheet)
        Assert.assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        Assert.assertEquals("PA0000005", seal.getFeature("SHEET_NUMBER")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.emergenyTravelDoc)
        Assert.assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    @Throws(IOException::class)
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.addressStickerId)
        Assert.assertEquals("T2000AK47", seal!!.getFeature("DOCUMENT_NUMBER")!!.valueStr)
        Assert.assertEquals("05314000", seal.getFeature("AGS")!!.valueStr)
        Assert.assertEquals("53175HEINEMANNSTR11", seal.getFeature("ADDRESS")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.addressStickerPassport)
        Assert.assertEquals("PA5500K11", seal!!.getFeature("DOCUMENT_NUMBER")!!.valueStr)
        Assert.assertEquals("03359010", seal.getFeature("AGS")!!.valueStr)
        Assert.assertEquals("21614", seal.getFeature("POSTAL_CODE")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.visa_224bitSig)
        Assert.assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<", seal!!.getFeature(
                "MRZ_MRVB"
            )!!
                .valueStr
        )
        Assert.assertEquals("47110815P", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        Assert.assertEquals(
            "a00000", Hex.toHexString(
                seal.getFeature("DURATION_OF_STAY")!!.valueBytes
            )
        )
        Assert.assertNull(seal.getFeature("NUMBER_OF_ENTRIES"))
    }

    @Test
    @Throws(IOException::class)
    fun testParseFictionCert() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.fictionCert)
        Assert.assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        Assert.assertEquals("X98723021", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        Assert.assertEquals("160113000085", seal.getFeature("AZR")!!.valueStr)
    }

    @Test
    @Throws(IOException::class)
    fun testParseTempPerso() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPerso)
        Assert.assertEquals(
            "ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<\nD000000001D<<8308126<2701312<<<<<<<0",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        val imgBytes = seal.getFeature("FACE_IMAGE")!!.valueBytes

        Assert.assertEquals(891, imgBytes.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testParseTempPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPassport)
        Assert.assertEquals(
            "PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<\nA000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetFeatureList() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.fictionCert)
        Assert.assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        Assert.assertEquals(4, seal.featureList.size.toLong())
        for (feature in seal.featureList) {
            if (feature.name == "AZR") {
                Assert.assertEquals("160113000085", feature.valueStr)
            }
            if (feature.name == "PASSPORT_NUMBER") {
                Assert.assertEquals("X98723021", feature.valueStr)
            }
        }
        Assert.assertNull(seal.getFeature("DURATION_OF_STAY"))
    }

    @Test
    fun testGetFeatureList2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPerso)
        val featureList = seal!!.featureList
        for (feature in featureList) {
            Logger.d(feature.name + ", " + feature.coding + ", " + feature.valueStr)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit)
        Assert.assertTrue(Arrays.areEqual(VdsRawBytes.residentPermit, seal!!.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestation)
        println(Hex.toHexString(VdsRawBytes.arrivalAttestation))
        println(Hex.toHexString(seal!!.encoded))
        Assert.assertTrue(Arrays.areEqual(VdsRawBytes.arrivalAttestation, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02)
        println(Hex.toHexString(VdsRawBytes.arrivalAttestationV02))
        println(Hex.toHexString(seal!!.encoded))
        Assert.assertTrue(Arrays.areEqual(VdsRawBytes.arrivalAttestationV02, seal.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedBytes_fc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.fictionCert)
        Assert.assertTrue(Arrays.areEqual(VdsRawBytes.fictionCert, seal!!.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02)
        val rawString = seal!!.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        Assert.assertEquals(rawString, seal2!!.rawString)
        Assert.assertEquals(
            Hex.toHexString(VdsRawBytes.arrivalAttestationV02), Hex.toHexString(
                seal2.encoded
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun testgetRawString2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPerso)
        val rawString = seal!!.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        Assert.assertEquals(rawString, seal2!!.rawString)
        Assert.assertEquals(
            Hex.toHexString(VdsRawBytes.tempPerso), Hex.toHexString(
                seal2.encoded
            )
        )
    }

    @Test
    @Throws(IOException::class, KeyStoreException::class)
    fun testBuildDigitalSeal() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessage.Builder("RESIDENCE_PERMIT")
            .addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("PASSPORT_NUMBER", passportNumber)
            .build()

        val ecPrivKey = SignerTest.keystore.getKey(
            "dets32",
            SignerTest.keyStorePassword.toCharArray()
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
        val ecPrivKey = SignerTest.keystore.getKey(
            "dets32",
            SignerTest.keyStorePassword.toCharArray()
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
    fun testUnknowSealType() {
        val rawBytes = VdsRawBytes.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = DigitalSeal.fromByteArray(rawBytes)
        Assert.assertNotNull(seal)
        Assert.assertNotNull(seal!!.vdsType)
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
