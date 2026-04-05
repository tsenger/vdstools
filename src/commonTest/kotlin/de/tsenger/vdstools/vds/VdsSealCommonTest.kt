package de.tsenger.vdstools.vds

import de.tsenger.vdstools.generic.MessageValue
import kotlinx.datetime.LocalDate
import okio.Buffer
import kotlin.test.*


@OptIn(ExperimentalStdlibApi::class)
class VdsSealCommonTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessageByName("SOCIAL_INSURANCE_NUMBER")?.value.toString())

        assertEquals("Perschweiß", seal.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Oscar", seal.getMessageByName("FIRST_NAME")?.value.toString())
        assertEquals("Jâcobénidicturius", seal.getMessageByName("BIRTH_NAME")?.value.toString())
    }

    @Test
    fun testParseSocialInsurranceCard_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        assertEquals("65170839J003", seal.getMessageByName("SOCIAL_INSURANCE_NUMBER")?.value.toString())

        assertEquals("Perschweiß", seal.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Oscar", seal.getMessageByName("FIRST_NAME")?.value.toString())
        assertEquals("Jâcobénidicturius", seal.getMessageByName("BIRTH_NAME")?.value.toString())
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as VdsSeal
        assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("0004F", seal.certificateReference)
        assertEquals("ABC123456DEF", seal.getMessageByName("AZR")?.value.toString())
        assertNull(seal.getMessageByName("FIRST_NAME"))
    }

    @Test
    fun testParseArrivalAttestationV02_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02) as VdsSeal
        assertEquals(
            "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("0004F", seal.certificateReference)
        assertEquals("ABC123456DEF", seal.getMessageByName("AZR")?.value.toString())
        assertNull(seal.getMessageByName("FIRST_NAME"))
    }

    @Test
    fun testParseResidentPermit() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("UFO001979", seal.getMessageByName("PASSPORT_NUMBER")!!.value.toString())
    }

    @Test
    fun testParseResidentPermit_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("UFO001979", seal.getMessageByName("PASSPORT_NUMBER")!!.value.toString())
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.supplementSheet) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("PA0000005", seal.getMessageByName("SHEET_NUMBER")!!.value.toString())
    }

    @Test
    fun testParseSupplementSheet_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.supplementSheet) as VdsSeal
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
        assertEquals("PA0000005", seal.getMessageByName("SHEET_NUMBER")!!.value.toString())
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc) as VdsSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
    }

    @Test
    fun testEmergencyTravelDoc_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc) as VdsSeal
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessageByName("MRZ")?.value.toString()
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerId) as VdsSeal
        assertEquals("T2000AK47", seal.getMessageByName("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("05314000", seal.getMessageByName("AGS")?.value.toString())
        assertEquals("53175HEINEMANNSTR11", seal.getMessageByName("ADDRESS")?.value.toString())
    }

    @Test
    fun testParseAddressStickerId_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerId) as VdsSeal
        assertEquals("T2000AK47", seal.getMessageByName("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("05314000", seal.getMessageByName("AGS")?.value.toString())
        assertEquals("53175HEINEMANNSTR11", seal.getMessageByName("ADDRESS")?.value.toString())
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerPassport) as VdsSeal
        assertEquals("PA5500K11", seal.getMessageByName("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("03359010", seal.getMessageByName("AGS")?.value.toString())
        assertEquals("21614", seal.getMessageByName("POSTAL_CODE")?.value.toString())
    }

    @Test
    fun testParseAddressStickerPassport_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.addressStickerPassport) as VdsSeal
        assertEquals("PA5500K11", seal.getMessageByName("DOCUMENT_NUMBER")?.value.toString())
        assertEquals("03359010", seal.getMessageByName("AGS")?.value.toString())
        assertEquals("21614", seal.getMessageByName("POSTAL_CODE")?.value.toString())
    }


    @Test
    fun testParseVisa() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.visa_224bitSig) as VdsSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessageByName("MRZ_MRVB")?.value.toString()
        )
        assertEquals("47110815P", seal.getMessageByName("PASSPORT_NUMBER")?.value.toString())
        assertEquals(
            "a00000", seal.getMessageByName("DURATION_OF_STAY")?.value?.rawBytes?.toHexString()
        )
        assertNull(seal.getMessageByName("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testParseVisa_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.visa_224bitSig) as VdsSeal
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
            seal.getMessageByName("MRZ_MRVB")?.value.toString()
        )
        assertEquals("47110815P", seal.getMessageByName("PASSPORT_NUMBER")?.value.toString())
        assertEquals(
            "a00000", seal.getMessageByName("DURATION_OF_STAY")?.value?.rawBytes?.toHexString()
        )
        assertNull(seal.getMessageByName("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testWrongFormatedCertificateReferenceLengthCoding() {
        val rawBytes =
            "dc036abc6d38dbb519a620372ce13372401c46ad535759e866926d2379b98d7ad88d7ad801c800109a4223406d374ef99e2cf95e31a2384604094c656965726d616e6e06074c6f72656e7a6fff40191d2ab504d5b6f9cda382857aeab508db1178463225bda4efac6ea64e803bb23c65e11d3ffae6f469feaa540d63ea6f612d4a4ba7f016a64ec39c5caf936bc7".hexToByteArray()
        val seal = VdsSeal.fromByteArray(rawBytes) as VdsSeal
        assertNotNull(seal)
        assertEquals("00112233445566778899AABBCCDDEEFF", seal.certificateReference)
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
        assertEquals(LocalDate.parse("2016-05-23"), seal.signingDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = VdsSeal.fromRawString(rawString) as VdsSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.signingDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal2.issuingDate)
        assertContentEquals(VdsRawBytesCommon.ankunftsnwPapier, seal2.encoded)
    }

    @Test
    fun testAnkunftsNwPapier2() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.ankunftsnwPapier2) as VdsSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal.signingDate)
        assertEquals(LocalDate.parse("2016-02-01"), seal.issuingDate)
        val rawString = seal.rawString
        val seal2 = VdsSeal.fromRawString(rawString) as VdsSeal
        assertNotNull(seal2)
        assertEquals("ARRIVAL_ATTESTATION", seal2.documentType)
        assertEquals(LocalDate.parse("2016-05-23"), seal2.signingDate)
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
        assertEquals(LocalDate.parse("2025-05-14"), seal.signingDate)
        assertEquals(LocalDate.parse("2025-05-14"), seal.issuingDate)
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Dr.", seal.getMessageByName("ACADEMIC_DEGREE")?.value.toString())
        assertEquals("Erika", seal.getMessageByName("FIRST_NAME")?.value.toString())
        assertEquals("20250414", seal.getMessageByName("MOVING_DATE")?.value.toString())
        assertEquals("20250504", seal.getMessageByName("DATE_OF_NOTIFICATION")?.value.toString())
        assertTrue(seal.getMessageByName("MOVING_DATE")?.value is MessageValue.StringValue)
    }

    @Test
    fun testMeldebescheinigung_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertNotNull(seal)
        println(seal.docTypeCat)
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)
        assertEquals(LocalDate.parse("2025-05-14"), seal.signingDate)
        assertEquals(LocalDate.parse("2025-05-14"), seal.issuingDate)
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
        assertEquals("Dr.", seal.getMessageByName("ACADEMIC_DEGREE")?.value.toString())
        assertEquals("Erika", seal.getMessageByName("FIRST_NAME")?.value.toString())
        assertEquals("20250414", seal.getMessageByName("MOVING_DATE")?.value.toString())
        assertEquals("20250504", seal.getMessageByName("DATE_OF_NOTIFICATION")?.value.toString())
        assertTrue(seal.getMessageByName("MOVING_DATE")?.value is MessageValue.StringValue)
    }

    @Test
    fun testMeldebescheinigung_tag0NotInMessageList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        // Tag 0 (DOC_PROFILE_NUMBER) should not appear in messageList
        assertNull(seal.getMessageByName("DOC_PROFILE_NUMBER"))
        assertNull(seal.getMessageByTag(0))
        // But documentProfileUuid should be set
        assertNotNull(seal.documentProfileUuid)
        assertEquals(16, seal.documentProfileUuid!!.size)
    }

    @Test
    fun testMeldebescheinigung_tag0NotInMessageList_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        // Tag 0 (DOC_PROFILE_NUMBER) should not appear in messageList
        assertNull(seal.getMessageByName("DOC_PROFILE_NUMBER"))
        assertNull(seal.getMessageByTag(0))
        // But documentProfileUuid should be set
        assertNotNull(seal.documentProfileUuid)
        assertEquals(16, seal.documentProfileUuid!!.size)
    }

    @Test
    fun testMeldebescheinigung_documentProfileUuid() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        val uuid = seal.documentProfileUuid
        assertNotNull(uuid)
        // UUID is the 16-byte DOC_PROFILE_NUMBER from the seal
        assertEquals("9a4223406d374ef99e2cf95e31a23846", uuid.toHexString())
    }

    @Test
    fun testParseMeldebescheinigungHeader() {
        val buffer = Buffer().write(VdsRawBytesCommon.meldebescheinigung)
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", header.vdsType)
        assertEquals("D  ", header.issuingCountry)
        assertEquals("DEZV", header.signerIdentifier)
        assertEquals("A41E7E495F0B4DE58AA0FE7C01D7FEA8", header.certificateReference)
        assertEquals("2025-05-14", header.issuingDate.toString())
        assertEquals("2025-05-14", header.sigDate.toString())
        assertEquals(LocalDate(2025, 5, 14), header.sigDate)
        assertEquals(1, header.docFeatureRef)
        assertEquals(0xc8.toByte(), header.docTypeCat)
    }

    @Test
    fun testParseMeldebescheinigungByTags() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("Mustermann", seal.getMessageByTag(4)?.value.toString())
        assertNull(seal.getMessageByTag(0)?.value)
        assertContentEquals("9a4223406d374ef99e2cf95e31a23846".hexToByteArray(), seal.documentProfileUuid)

    }

    @Test
    fun testParseMeldebescheinigungByTags_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("Mustermann", seal.getMessageByTag(4)?.value.toString())
        assertNull(seal.getMessageByTag(0)?.value)
        assertContentEquals("9a4223406d374ef99e2cf95e31a23846".hexToByteArray(), seal.documentProfileUuid)

    }

    @Test
    fun testParseMeldebescheinigungByNames() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
        assertNull(seal.getMessageByName("DOC_PROFILE_NUMBER")?.value)
        assertContentEquals("9a4223406d374ef99e2cf95e31a23846".hexToByteArray(), seal.documentProfileUuid)
    }

    @Test
    fun testParseMeldebescheinigungByNames_newApi() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("Mustermann", seal.getMessageByName("SURNAME")?.value.toString())
        assertNull(seal.getMessageByName("DOC_PROFILE_NUMBER")?.value)
        assertContentEquals("9a4223406d374ef99e2cf95e31a23846".hexToByteArray(), seal.documentProfileUuid)
    }

    @Test
    fun testMeldebescheinigung_metadataMessageListContainsUuidTag() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        val metaList = seal.metadataMessageList
        assertEquals(1, metaList.size)
        assertEquals(0x00, metaList[0].tag)
        assertEquals("DOC_PROFILE_NUMBER", metaList[0].name)
        assertContentEquals("9a4223406d374ef99e2cf95e31a23846".hexToByteArray(), metaList[0].value.rawBytes)
    }

    @Test
    fun testMeldebescheinigung_metadataTagAbsentFromMessageList() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        assertTrue(seal.messageList.none { it.tag == 0x00 })
    }

    @Test
    fun testRegularSeal_metadataMessageListIsEmpty() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertTrue(seal.metadataMessageList.isEmpty())
    }

    @Test
    fun testMeldebescheinigung_messageListContainsNoBaseTags() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.meldebescheinigung) as VdsSeal
        // Tags 0-3 are base-type metadata tags and must not appear in messageList
        assertTrue(seal.messageList.none { it.tag in listOf(0x00, 0x01, 0x02, 0x03) })
    }

    @Test
    fun testMeldebescheinigung_builderMetadataTagsIncludeBaseTypeTags() {
        val builder = VdsMessageGroup.Builder("MELDEBESCHEINIGUNG")
        builder.addMessage("SURNAME", "Test")
        val group = builder.build()
        // metadataTags must contain all configured base-type metadata tags (0–3)
        assertTrue(0 in group.metadataTags)
        assertTrue(1 in group.metadataTags)
        assertTrue(2 in group.metadataTags)
        assertTrue(3 in group.metadataTags)
    }


}
