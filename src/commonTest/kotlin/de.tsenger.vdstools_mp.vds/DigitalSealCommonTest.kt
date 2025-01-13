package de.tsenger.vdstools_mp.vds

import co.touchlab.kermit.Logger
import kotlin.test.*


@OptIn(ExperimentalStdlibApi::class)
class DigitalSealCommonTest {
    @Test
    fun testParseSocialInsurranceCard() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.socialInsurance)
        assertEquals("SOCIAL_INSURANCE_CARD", seal!!.vdsType)
        assertEquals("65170839J003", seal.getFeature("SOCIAL_INSURANCE_NUMBER")!!.valueStr)
        assertEquals("Perschweiß", seal.getFeature("SURNAME")!!.valueStr)
        assertEquals("Oscar", seal.getFeature("FIRST_NAME")!!.valueStr)
        assertEquals("Jâcobénidicturius", seal.getFeature("BIRTH_NAME")!!.valueStr)
    }

    @Test
    fun testParseArrivalAttestationV02() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02)
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
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.residentPermit)
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("UFO001979", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
    }

    @Test
    fun testParseSupplementSheet() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.supplementSheet)
        assertEquals(
            "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("PA0000005", seal.getFeature("SHEET_NUMBER")!!.valueStr)
    }

    @Test
    fun testEmergencyTravelDoc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc)
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    fun testParseAddressStickerId() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.addressStickerId)
        assertEquals("T2000AK47", seal?.getFeature("DOCUMENT_NUMBER")?.valueStr)
        assertEquals("05314000", seal?.getFeature("AGS")?.valueStr)
        assertEquals("53175HEINEMANNSTR11", seal?.getFeature("ADDRESS")?.valueStr)
    }

    @Test
    fun testParseAddressStickerPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.addressStickerPassport)
        assertEquals("PA5500K11", seal!!.getFeature("DOCUMENT_NUMBER")!!.valueStr)
        assertEquals("03359010", seal.getFeature("AGS")!!.valueStr)
        assertEquals("21614", seal.getFeature("POSTAL_CODE")!!.valueStr)
    }


    @Test
    fun testParseVisa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.visa_224bitSig)
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<", seal!!.getFeature(
                "MRZ_MRVB"
            )!!
                .valueStr
        )
        assertEquals("47110815P", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        assertEquals(
            "a00000", seal.getFeature("DURATION_OF_STAY")!!.valueBytes.toHexString()
        )
        assertNull(seal.getFeature("NUMBER_OF_ENTRIES"))
    }

    @Test
    fun testParseFictionCert() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.fictionCert)
        assertEquals(
            "NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<\nL000000007TUR8308126F2701312T2611011",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        assertEquals("X98723021", seal.getFeature("PASSPORT_NUMBER")!!.valueStr)
        assertEquals("160113000085", seal.getFeature("AZR")!!.valueStr)
    }

    @Test
    fun testParseTempPerso() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.tempPerso)
        assertEquals(
            "ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<\nD000000001D<<8308126<2701312<<<<<<<0",
            seal!!.getFeature("MRZ")!!.valueStr
        )
        val imgBytes = seal.getFeature("FACE_IMAGE")!!.valueBytes

        assertEquals(891, imgBytes.size.toLong())
    }

    @Test
    fun testParseTempPassport() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.tempPassport)
        assertEquals(
            "PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<\nA000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
            seal!!.getFeature("MRZ")!!.valueStr
        )
    }

    @Test
    fun testGetFeatureList() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.fictionCert)
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
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.tempPerso)
        val featureList = seal?.featureList
        assertNotNull(featureList)
        for (feature in featureList) {
            Logger.d(feature.name + ", " + feature.coding + ", " + feature.valueStr)
        }
    }

    @Test
    fun testGetEncodedBytes_rp() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.residentPermit)
        assertContentEquals(VdsRawBytesCommon.residentPermit, seal?.encoded)
    }

    @Test
    fun testGetEncodedBytes_aa() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestation)
        assertContentEquals(VdsRawBytesCommon.arrivalAttestation, seal?.encoded)
    }

    @Test
    fun testGetEncodedBytes_aav2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02)

        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal?.encoded)
    }

    @Test
    fun testGetEncodedBytes_fc() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.fictionCert)
        assertContentEquals(VdsRawBytesCommon.fictionCert, seal?.encoded)
    }

    @Test
    fun testgetRawString1() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.arrivalAttestationV02)
        assertNotNull(seal)
        val rawString = seal.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        assertEquals(rawString, seal2?.rawString)
        assertContentEquals(VdsRawBytesCommon.arrivalAttestationV02, seal2?.encoded)
    }

    @Test
    fun testgetRawString2() {
        val seal = DigitalSeal.fromByteArray(VdsRawBytesCommon.tempPerso)
        val rawString = seal!!.rawString
        val seal2 = DigitalSeal.fromRawString(rawString)
        assertEquals(rawString, seal2!!.rawString)
        assertContentEquals(VdsRawBytesCommon.tempPerso, seal2.encoded)
    }


    @Test
    fun testUnknowSealType() {
        val rawBytes = VdsRawBytesCommon.permanentResidencePermit
        rawBytes[16] = 0x99.toByte()
        val seal = DigitalSeal.fromByteArray(rawBytes)
        assertNotNull(seal)
        assertNotNull(seal.vdsType)
    }


}
