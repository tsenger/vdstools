package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.getCryptoProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class DigitalSeaIoslTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.socialInsurance) as DigitalSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.value.toString())
        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Oscar", seal.getMessage("FIRST_NAME")?.value.toString())
        assertEquals("Jâcobénidicturius", seal.getMessage("BIRTH_NAME")?.value.toString())
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.arrivalAttestationV02) as DigitalSeal
        assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
        assertEquals("0004F", seal.certificateReference)
        assertEquals("ABC123456DEF", seal.getMessage("AZR")?.value.toString())
        assertNull(seal.getMessage("FIRST_NAME"))
    }

    @Test
    fun testParseResidentPermit() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.residentPermit) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
        assertEquals("UFO001979", seal.getMessage("PASSPORT_NUMBER")?.value.toString())
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.supplementSheet) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
        assertEquals("PA0000005", seal.getMessage("SHEET_NUMBER")?.value.toString())
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.emergenyTravelDoc) as DigitalSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerId) as DigitalSeal
        assertEquals("T2000AK47", seal.getMessage("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("05314000", seal.getMessage("AGS")?.value.toString())
        assertEquals("53175HEINEMANNSTR11", seal.getMessage("ADDRESS")?.value.toString())
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.addressStickerPassport) as DigitalSeal
        assertEquals("PA5500K11", seal.getMessage("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("03359010", seal.getMessage("AGS")?.value.toString())
        assertEquals("21614", seal.getMessage("POSTAL_CODE")?.value.toString())
    }

    @Test
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesIos.visa_224bitSig) as DigitalSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessage("MRZ_MRVB")?.value.toString()
        )
        assertEquals("47110815P", seal.getMessage("PASSPORT_NUMBER")?.value.toString())
        assertEquals("a00000", seal.getMessage("DURATION_OF_STAY")?.value.toString())
        assertNull(seal.getMessage("NUMBER_OF_ENTRIES"))
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

    @OptIn(ExperimentalStdlibApi::class, ExperimentalTime::class)
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
