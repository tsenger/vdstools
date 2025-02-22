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
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.socialInsurance) as DigitalSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.valueStr)
        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.valueStr)
        assertEquals("Oscar", seal.getMessage("FIRST_NAME")?.valueStr)
        assertEquals("Jâcobénidicturius", seal.getMessage("BIRTH_NAME")?.valueStr)
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02) as DigitalSeal
        assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("0004F", seal.certificateReference)
        assertEquals("ABC123456DEF", seal.getMessage("AZR")?.valueStr)
        assertNull(seal.getMessage("FIRST_NAME"))
    }

    @Test
    fun testParseResidentPermit() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("UFO001979", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.supplementSheet) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("PA0000005", seal.getMessage("SHEET_NUMBER")?.valueStr)
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.emergenyTravelDoc) as DigitalSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerId) as DigitalSeal
        assertEquals("T2000AK47", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        assertEquals("05314000", seal.getMessage("AGS")?.valueStr)
        assertEquals("53175HEINEMANNSTR11", seal.getMessage("ADDRESS")?.valueStr)
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerPassport) as DigitalSeal
        assertEquals("PA5500K11", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        assertEquals("03359010", seal.getMessage("AGS")?.valueStr)
        assertEquals("21614", seal.getMessage("POSTAL_CODE")?.valueStr)
    }

    @Test
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.visa_224bitSig) as DigitalSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessage("MRZ_MRVB")?.valueStr
        )
        assertEquals("47110815P", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
        assertEquals("a00000", seal.getMessage("DURATION_OF_STAY")?.valueStr)
        assertNull(seal.getMessage("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testParseFictionCert() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert) as DigitalSeal
        assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("X98723021", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
        assertEquals("160113000085", seal.getMessage("AZR")?.valueStr)
    }

    @Test
    fun testParseTempPerso() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso) as DigitalSeal
        assertEquals(
            "ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<\nD000000001D<<8308126<2701312<<<<<<<0",
            seal.getMessage("MRZ")?.valueStr
        )
        val imgBytes = seal.getMessage("FACE_IMAGE")?.valueBytes

        assertEquals(891, imgBytes?.size)
    }

    @Test
    fun testParseTempPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPassport) as DigitalSeal
        assertEquals(
            "PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<\nA000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
            seal.getMessage("MRZ")?.valueStr
        )
    }

    @Test
    fun testgetMessageList() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert) as DigitalSeal
        assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal.getMessage("MRZ")?.valueStr
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
        assertNull(seal.getMessage("DURATION_OF_STAY"))
    }

    @Test
    fun testgetMessageList2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso) as DigitalSeal
        val featureList = seal.featureList
        for (feature in featureList) {
            Logger.d(feature.name + ", " + feature.coding + ", " + feature.valueStr)
        }
    }

    @Test
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit) as DigitalSeal
        assertContentEquals(VdsRawBytesIos.residentPermit, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestation) as DigitalSeal
        assertContentEquals(VdsRawBytesIos.arrivalAttestation, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02) as DigitalSeal
        assertContentEquals(VdsRawBytesIos.arrivalAttestationV02, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_fc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.fictionCert) as DigitalSeal
        assertContentEquals(VdsRawBytesIos.fictionCert, seal.encoded)
    }

    @Test
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02) as DigitalSeal
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        assertEquals(rawString, seal2.rawString)
        assertContentEquals(
            VdsRawBytesIos.arrivalAttestationV02, seal2.encoded
        )
    }

    @Test
    fun testgetRawString2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.tempPerso) as DigitalSeal
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        assertEquals(rawString, seal2.rawString)
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
        val seal = DigitalSeal.fromByteArray(rawBytes) as DigitalSeal
        assertNotNull(seal)
        assertNotNull(seal.documentType)
    }


}
