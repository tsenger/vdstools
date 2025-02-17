package de.tsenger.vdstools


import de.tsenger.vdstools.DataEncoder.decodeDateTime
import de.tsenger.vdstools.DataEncoder.decodeMaskedDate
import de.tsenger.vdstools.vds.VdsHeader
import de.tsenger.vdstools.vds.VdsRawBytesIos

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import okio.Buffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class DataParserIosTest {
    @Test
    fun testDecodeMaskedDate1() {
        val decodedDate = decodeMaskedDate("c3002e7c".hexToByteArray())
        assertEquals("19xx-xx-01", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate2() {
        val decodedDate = decodeMaskedDate("313d10da".hexToByteArray())
        assertEquals("201x-04-xx", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate3() {
        val decodedDate = decodeMaskedDate("f000076c".hexToByteArray())
        assertEquals("1900-xx-xx", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate4() {
        val decodedDate = decodeMaskedDate("00bbddbf".hexToByteArray())
        assertEquals("1999-12-31", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate5() {
        val decodedDate = decodeMaskedDate("ff000000".hexToByteArray())
        assertEquals("xxxx-xx-xx", decodedDate)
    }

    @Test
    fun testDecodeMaskedDate6_invalidFormat() {
        assertFailsWith<IllegalArgumentException> {
            decodeMaskedDate("ff0000".hexToByteArray())
        }
    }

    @Test
    fun testDecodeDateTime1() {
        val localDateTime = decodeDateTime("0aecc4c7fb80".hexToByteArray())
        println(localDateTime)
        assertEquals(LocalDateTime.parse("2030-12-01T00:00:00"), localDateTime)
    }

    @Test
    fun testDecodeDateTime2() {
        val localDateTime = decodeDateTime("02f527bf25b2".hexToByteArray())
        println(localDateTime)
        assertEquals(LocalDateTime.parse("1957-03-25T08:15:22"), localDateTime)
    }

    @Test
    fun testDecodeDateTime3() {
        val localDateTime = decodeDateTime("00eb28c03640".hexToByteArray())
        println(localDateTime)
        assertEquals(LocalDateTime.parse("0001-01-01T00:00:00"), localDateTime)
    }

    @Test
    fun testDecodeDateTime4() {
        val localDateTime = decodeDateTime("0b34792d9777".hexToByteArray())
        println(localDateTime)
        assertEquals(LocalDateTime.parse("9999-12-31T23:59:59"), localDateTime)
    }

    @Test
    fun testDecodeHeader() {
        val bb = Buffer().write(VdsRawBytesIos.residentPermit)
        val vdsHeader = VdsHeader.fromBuffer(bb)
        assertEquals(0x03, vdsHeader.rawVersion.toLong())
        assertEquals("UTO", vdsHeader.issuingCountry)
        assertEquals("UTTS", vdsHeader.signerIdentifier)
        assertEquals("5B", vdsHeader.certificateReference)
        assertEquals("UTTS5B", vdsHeader.signerCertRef)
        assertEquals(LocalDate.parse("2020-01-01").toString(), vdsHeader.issuingDate.toString())
        assertEquals(LocalDate.parse("2023-07-26").toString(), vdsHeader.sigDate.toString())
        assertEquals(0xfb, (vdsHeader.docFeatureRef.toInt() and 0xff).toLong())
        assertEquals(0x06, (vdsHeader.docTypeCat.toInt() and 0xff).toLong())
    }

    @Test
    fun testUnzip() {
        val compressedBytes = (
                "78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b"
                ).hexToByteArray()
        val decompressedBytes = DataEncoder.unzip(compressedBytes)
        assertEquals(
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            decompressedBytes.toHexString()
        )
    }
}