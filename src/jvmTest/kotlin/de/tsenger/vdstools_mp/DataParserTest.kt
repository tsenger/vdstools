package de.tsenger.vdstools_mp


import de.tsenger.vdstools_mp.DataParser.decodeDateTime
import de.tsenger.vdstools_mp.DataParser.decodeMaskedDate
import de.tsenger.vdstools_mp.vds.VdsHeader
import de.tsenger.vdstools_mp.vds.VdsRawBytes
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import okio.Buffer
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class DataParserTest {
    @Test
    fun testDecodeMaskedDate1() {
        val decodedDate = decodeMaskedDate(Hex.decode("c3002e7c"))
        Assert.assertEquals("19xx-xx-01", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate2() {
        val decodedDate = decodeMaskedDate(Hex.decode("313d10da"))
        Assert.assertEquals("201x-04-xx", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate3() {
        val decodedDate = decodeMaskedDate(Hex.decode("f000076c"))
        Assert.assertEquals("1900-xx-xx", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate4() {
        val decodedDate = decodeMaskedDate(Hex.decode("00bbddbf"))
        Assert.assertEquals("1999-12-31", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate5() {
        val decodedDate = decodeMaskedDate(Hex.decode("ff000000"))
        Assert.assertEquals("xxxx-xx-xx", decodedDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDecodeMaskedDate6_invalidFormat() {
        val decodedDate = decodeMaskedDate(Hex.decode("ff0000"))
        Assert.assertNull(decodedDate)
    }

    @Test
    fun testDecodeDateTime1() {
        val localDateTime = decodeDateTime(Hex.decode("0aecc4c7fb80"))
        println(localDateTime)
        assertEquals(LocalDateTime.parse("2030-12-01T00:00:00"), localDateTime)
    }

    @Test
    fun testDecodeDateTime2() {
        val localDateTime = decodeDateTime(Hex.decode("02f527bf25b2"))
        println(localDateTime)
        assertEquals(LocalDateTime.parse("1957-03-25T08:15:22"), localDateTime)
    }

    @Test
    fun testDecodeDateTime3() {
        val localDateTime = decodeDateTime(Hex.decode("00eb28c03640"))
        println(localDateTime)
        assertEquals(LocalDateTime.parse("0001-01-01T00:00:00"), localDateTime)
    }

    @Test
    fun testDecodeDateTime4() {
        val localDateTime = decodeDateTime(Hex.decode("0b34792d9777"))
        println(localDateTime)
        assertEquals(LocalDateTime.parse("9999-12-31T23:59:59"), localDateTime)
    }

    @Test
    fun testDecodeHeader() {
        val bb = Buffer().write(VdsRawBytes.residentPermit)
        val vdsHeader = VdsHeader.fromBuffer(bb)
        Assert.assertEquals(0x03, vdsHeader.rawVersion.toLong())
        Assert.assertEquals("UTO", vdsHeader.issuingCountry)
        Assert.assertEquals("UTTS", vdsHeader.signerIdentifier)
        Assert.assertEquals("5B", vdsHeader.certificateReference)
        Assert.assertEquals("UTTS5B", vdsHeader.signerCertRef)
        Assert.assertEquals(LocalDate.parse("2020-01-01").toString(), vdsHeader.issuingDate.toString())
        Assert.assertEquals(LocalDate.parse("2023-07-26").toString(), vdsHeader.sigDate.toString())
        Assert.assertEquals(0xfb, (vdsHeader.docFeatureRef.toInt() and 0xff).toLong())
        Assert.assertEquals(0x06, (vdsHeader.docTypeCat.toInt() and 0xff).toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testUnzip() {
        val compressedBytes = Hex.decode(
            "78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b"
        )
        val decompressedBytes = DataParser.unzip(compressedBytes)
        println("Decompressed: " + Hex.toHexString(decompressedBytes))
        Assert.assertEquals(
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            Hex.toHexString(decompressedBytes)
        )
    }
}