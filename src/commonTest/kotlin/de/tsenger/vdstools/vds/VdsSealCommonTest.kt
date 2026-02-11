package de.tsenger.vdstools.vds

import kotlinx.datetime.LocalDate
import kotlin.test.*


@OptIn(ExperimentalStdlibApi::class)
class VdsSealCommonTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessage("SOCIAL_INSURANCE_NUMBER")?.value.toString())

        assertEquals("Perschweiß", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Oscar", seal.getMessage("FIRST_NAME")?.value.toString())
        assertEquals("Jâcobénidicturius", seal.getMessage("BIRTH_NAME")?.value.toString())
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as VdsSeal
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
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
        assertEquals("UFO001979", seal.getMessage("PASSPORT_NUMBER")!!.value.toString())
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.supplementSheet) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
        assertEquals("PA0000005", seal.getMessage("SHEET_NUMBER")!!.value.toString())
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc) as VdsSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage("MRZ")?.value.toString()
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerId) as VdsSeal
        assertEquals("T2000AK47", seal.getMessage("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("05314000", seal.getMessage("AGS")?.value.toString())
        assertEquals("53175HEINEMANNSTR11", seal.getMessage("ADDRESS")?.value.toString())
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerPassport) as VdsSeal
        assertEquals("PA5500K11", seal.getMessage("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("03359010", seal.getMessage("AGS")?.value.toString())
        assertEquals("21614", seal.getMessage("POSTAL_CODE")?.value.toString())
    }


    @Test
    fun testParseVisa() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.visa_224bitSig) as VdsSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessage("MRZ_MRVB")?.value.toString()
        )
        assertEquals("47110815P", seal.getMessage("PASSPORT_NUMBER")?.value.toString())
        assertEquals(
            "a00000", seal.getMessage("DURATION_OF_STAY")?.value?.rawBytes?.toHexString()
        )
        assertNull(seal.getMessage("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testGetEncodedBytes_rp() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertContentEquals(VdsRawBytesCommon.residentPermit, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aa() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestation) as VdsSeal
        assertContentEquals(VdsRawBytesCommon.arrivalAttestation, seal.encoded)
    }

    @Test
    fun testGetEncodedBytes_aav2() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as VdsSeal

        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal.encoded)
    }

    @Test
    fun testgetRawString1() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as VdsSeal
        assertNotNull(seal)
        val rawString = seal.rawString
        val seal2 = VdsSeal.fromRawString(rawString) as VdsSeal
        assertEquals(rawString, seal2.rawString)
        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal2.encoded)
    }

    @Test
    fun testUnknownSealType() {
        val rawBytes = VdsRawBytesCommon.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = VdsSeal.fromByteArray(rawBytes) as VdsSeal
        assertNotNull(seal)
        assertNotNull(seal.documentType)
        assertEquals("UNKNOWN", seal.documentType)
    }

    @Test
    fun testAnkunftsNwPapier() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.ankunftsnwPapier) as VdsSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = VdsSeal.fromRawString(rawString) as VdsSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal2.issuingDate)
        assertContentEquals(VdsRawBytesCommon.ankunftsnwPapier, seal2.encoded)
    }

    @Test
    fun testAnkunftsNwPapier2() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.ankunftsnwPapier2) as VdsSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = VdsSeal.fromRawString(rawString) as VdsSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.sigDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal2.issuingDate)
        assertContentEquals(VdsRawBytesCommon.ankunftsnwPapier2, seal2.encoded)
    }

    @Test
    fun testMeldebescheinigung() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)
        assertEquals(LocalDate.parse("2025-05-14"), seal.sigDate)
        assertEquals(LocalDate.parse("2025-05-14"), seal.issuingDate)
        assertEquals("Mustermann", seal.getMessage("SURNAME")?.value.toString())
        assertEquals("Dr.", seal.getMessage("ACADEMIC_DEGREE")?.value.toString())
        assertEquals("Erika", seal.getMessage("FIRST_NAME")?.value.toString())
        assertEquals("20250414", seal.getMessage("MOVING_DATE")?.value.toString())
        assertEquals("20250504", seal.getMessage("DATE_OF_NOTIFICATION")?.value.toString())
    }


}
