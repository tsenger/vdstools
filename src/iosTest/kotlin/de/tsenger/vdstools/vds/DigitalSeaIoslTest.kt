package de.tsenger.vdstools.vds

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.getCryptoProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.*


class DigitalSeaIoslTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.socialInsurance)
        assertEquals("SOCIAL_INSURANCE_CARD", seal!!.vdsType)
        assertEquals("65170839J003", seal.getFeature("SOCIAL_INSURANCE_NUMBER")!!.valueStr)
        assertEquals("Perschweiß", seal.getFeature("SURNAME")!!.valueStr)
        assertEquals("Oscar", seal.getFeature("FIRST_NAME")!!.valueStr)
        assertEquals("Jâcobénidicturius", seal.getFeature("BIRTH_NAME")!!.valueStr)
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02)
        assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("0004F", seal.certificateReference)
        assertEquals("ABC123456DEF", seal.getFeature("AZR")!!.valueStr)
        assertNull(seal.getFeature("FIRST_NAME"))
    }

    @Test
    fun testParseResidentPermit() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit)
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("UFO001979", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.supplementSheet)
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("PA0000005", seal.getFeature("SHEET_NUMBER")!!.valueStr)
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.emergenyTravelDoc)
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerId)
        assertEquals("T2000AK47", seal!!.getFeature("DOCUMENT_NUMBER")!!.valueStr)
        assertEquals("05314000", seal.getFeature("AGS")!!.valueStr)
        assertEquals("53175HEINEMANNSTR11", seal.getFeature("ADDRESS")!!.valueStr)
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerPassport)
        assertEquals("PA5500K11", seal!!.getFeature("DOCUMENT_NUMBER")!!.valueStr)
        assertEquals("03359010", seal.getFeature("AGS")!!.valueStr)
        assertEquals("21614", seal.getFeature("POSTAL_CODE")!!.valueStr)
    }

    @Test
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.visa_224bitSig)
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<", seal!!.getFeature(
                "MRZ_MRVB"
            )!!
                .valueStr
        )
        assertEquals("47110815P", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        assertEquals(
            "a00000", (
                    seal.getFeature("DURATION_OF_STAY")!!.valueStr
                    )
        )
        assertNull(seal.getFeature("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testParseFictionCert() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert)
        assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("X98723021", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        assertEquals("160113000085", seal.getFeature("AZR")!!.valueStr)
    }

    @Test
    fun testParseTempPerso() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso)
        assertEquals(
            "ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<\nD000000001D<<8308126<2701312<<<<<<<0",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        val imgBytes = seal.getFeature("FACE_IMAGE")!!.valueBytes

        assertEquals(891, imgBytes.size.toLong())
    }

    @Test
    fun testParseTempPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPassport)
        assertEquals(
            "PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<\nA000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    fun testGetFeatureList() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert)
        assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals(4, seal.featureList.size.toLong())
        for (feature in seal.featureList) {
            if (feature.name == "AZR") {
                assertEquals("160113000085", feature.valueStr)
            }
            if (feature.name == "PASSPORT_NUMBER") {
                assertEquals("X98723021", feature.valueStr)
            }
        }
        assertNull(seal.getFeature("DURATION_OF_STAY"))
    }

    @Test
    fun testGetFeatureList2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso)
        val featureList = seal!!.featureList
        for (feature in featureList) {
            Logger.d(feature.name + ", " + feature.coding + ", " + feature.valueStr)
        }
    }

    @Test
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit)
        assertContentEquals(VdsRawBytesIos.residentPermit, seal!!.encoded)
    }

    @Test
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestation)
        assertContentEquals(VdsRawBytesIos.arrivalAttestation, seal?.encoded)
    }

    @Test
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02)
        assertContentEquals(VdsRawBytesIos.arrivalAttestationV02, seal?.encoded)
    }

    @Test
    fun testGetEncodedBytes_fc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert)
        assertContentEquals(VdsRawBytesIos.fictionCert, seal!!.encoded)
    }

    @Test
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02)
        val rawString = seal!!.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        assertEquals(rawString, seal2!!.rawString)
        assertContentEquals(
            VdsRawBytesIos.arrivalAttestationV02, seal2.encoded
        )
    }

    @Test
    fun testgetRawString2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso)
        val rawString = seal!!.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        assertEquals(rawString, seal2!!.rawString)
        assertContentEquals(
            VdsRawBytesIos.tempPerso, seal2.encoded
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBuildDigitalSeal() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessage.Builder("RESIDENCE_PERMIT")
            .addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("PASSPORT_NUMBER", passportNumber)
            .build()

        val keyPairGenerator = getCryptoProvider().get(ECDSA).keyPairGenerator(Curve("brainpoolP224r1"))
        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()
        val signer = Signer(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "brainpoolP224r1")

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder(vdsMessage.vdsType)
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()
        val digitalSeal = DigitalSeal(vdsHeader, vdsMessage, signer)
        assertNotNull(digitalSeal)
        val expectedHeaderMessage = (
                "dc036abc6d32c8a72cb1".hexToByteArray()
                        + encodedDate
                        + encodedDate
                        + "fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306".hexToByteArray()
                )
        val headerMessage = digitalSeal.encoded.slice(0..75).toByteArray()
        // System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
        assertContentEquals(expectedHeaderMessage, headerMessage)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test

    fun testBuildDigitalSeal2() {
        val keyPairGenerator = getCryptoProvider().get(ECDSA).keyPairGenerator(Curve("brainpoolP224r1"))
        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()
        val signer = Signer(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "brainpoolP224r1")
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

        assertNotNull(digitalSeal)
        val expectedHeaderMessage = (
                "dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24"
                ).hexToByteArray()
        val headerMessage = digitalSeal.encoded.slice(0..77).toByteArray()
        assertContentEquals(expectedHeaderMessage, headerMessage)
    }

    @Test
    fun testUnknowSealType() {
        val rawBytes = VdsRawBytesIos.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = DigitalSeal.fromByteArray(rawBytes)
        assertNotNull(seal)
        assertNotNull(seal.vdsType)
    }


}
