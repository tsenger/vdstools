package de.tsenger.vdstools.vds

import kotlinx.datetime.LocalDate
import kotlin.test.*


@OptIn(ExperimentalStdlibApi::class)
class DigitalSealCommonTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as DigitalSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.valueStr)

        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.valueStr)
        assertEquals("Oscar", seal.getMessage("FIRST_NAME")?.valueStr)
        assertEquals("Jâcobénidicturius", seal.getMessage("BIRTH_NAME")?.valueStr)
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as DigitalSeal
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
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("UFO001979", seal.getMessage("PASSPORT_NUMBER")!!.valueStr)
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.supplementSheet) as DigitalSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
        assertEquals("PA0000005", seal.getMessage("SHEET_NUMBER")!!.valueStr)
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc) as DigitalSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.valueStr
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.addressStickerId) as DigitalSeal
        assertEquals("T2000AK47", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        assertEquals("05314000", seal.getMessage("AGS")?.valueStr)
        assertEquals("53175HEINEMANNSTR11", seal.getMessage("ADDRESS")?.valueStr)
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.addressStickerPassport) as DigitalSeal
        assertEquals("PA5500K11", seal.getMessage("DOCUMENT_NUMBER")?.valueStr)
        assertEquals("03359010", seal.getMessage("AGS")?.valueStr)
        assertEquals("21614", seal.getMessage("POSTAL_CODE")?.valueStr)
    }


    @Test
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.visa_224bitSig) as DigitalSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessage("MRZ_MRVB")?.valueStr
        )
        assertEquals("47110815P", seal.getMessage("PASSPORT_NUMBER")?.valueStr)
        assertEquals(
            "a00000", seal.getMessage("DURATION_OF_STAY")?.valueBytes?.toHexString()
        )
        assertNull(seal.getMessage("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as DigitalSeal
        assertContentEquals(VdsRawBytesCommon.residentPermit, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestation) as DigitalSeal
        assertContentEquals(VdsRawBytesCommon.arrivalAttestation, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as DigitalSeal

        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal.encoded)
    }

    @Test
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as DigitalSeal
        assertNotNull(seal)
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        assertEquals(rawString, seal2.rawString)
        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal2.encoded)
    }

    @Test
    fun testUnknownSealType() {
        val rawBytes = VdsRawBytesCommon.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = DigitalSeal.fromByteArray(rawBytes) as DigitalSeal
        assertNotNull(seal)
        assertNotNull(seal.documentType)
    }

    @Test
    fun testAnkunftsNwPapier() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.ankunftsnwPapier) as DigitalSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal2.issuingDate)
        assertContentEquals(VdsRawBytesCommon.ankunftsnwPapier, seal2.encoded)
    }

    @Test
    fun testAnkunftsNwPapier2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.ankunftsnwPapier2) as DigitalSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString) as DigitalSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal2.issuingDate)
        assertContentEquals(VdsRawBytesCommon.ankunftsnwPapier2, seal2.encoded)
    }


}
