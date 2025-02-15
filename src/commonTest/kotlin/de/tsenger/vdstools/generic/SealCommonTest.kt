package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.idb.IcaoBarcode
import de.tsenger.vdstools.idb.IcbRawStringsCommon
import de.tsenger.vdstools.vds.DigitalSeal
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class SealCommonTest {

    @Test
    fun testFromStringIdb() {
        assertTrue(Seal.fromString(IcbRawStringsCommon.TemporaryPassport) is IcaoBarcode)
    }

    @Test
    fun testFromStringIdb_fail() {
        assertFalse(Seal.fromString(IcbRawStringsCommon.TemporaryPassport) is DigitalSeal)
    }


    @Test
    fun testIdbGetMessageTag() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        val mrz = seal.getMessage(8)?.valueStr
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
    }

    @Test
    fun testIdbGetMessageName() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        val mrz = seal.getMessage("MRZ_TD3")?.valueStr
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
    }

    @Test
    fun testIdbRawString() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals(IcbRawStringsCommon.TemporaryPassport, seal.rawString)
    }

    @Test
    fun testIdbGetPlainSignature() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        println(seal.getPlainSignature()?.toHexString())
        val signature = seal.getPlainSignature()
        assertNotNull(signature)
        assertEquals(64, signature.size)
        assertContentEquals(
            "5d31b07d744257e59bc43316cc6420d61464e5a0381897e99299813bfa7c857943edd6393ecd0bb74809f3a280c08156057000e93a1116eb1bf3336bbeb65c29".hexToByteArray(),
            signature
        )
    }

    @Test
    fun testFromStringVds() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        assertTrue(Seal.fromString(rawString) is DigitalSeal)
    }

    @Test
    fun testVdsGetMessageTag() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val mrz = seal.getMessage(2)?.valueStr
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
    }

    @Test
    fun testVdsGetMessageName() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val mrz = seal.getMessage("MRZ")?.valueStr
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
    }

    @Test
    fun testVdsRawString() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals(rawString, seal.rawString)
    }

    @Test
    fun testVdsGetPlainSignature() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val signature = seal.getPlainSignature()
        assertNotNull(signature)
        assertEquals(64, signature.size)
        assertContentEquals(
            "22f8bd19eccba4ef24f204787796dd914fec61f605b153b22a6ef307d3869938a4e7e908f0a63b8379880b395c7fdbac720d7f2836d08e1da62611614a00120b".hexToByteArray(),
            signature
        )
    }


}