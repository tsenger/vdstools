package de.tsenger.vdstools_mp


import kotlinx.datetime.LocalDateTime
import kotlin.test.Test

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class DataEncoderCommonTest {
    //	@Test
    //	public void testEncodeDate_Now() {
    //		LocalDate ldNow = LocalDate.now();
    //		System.out.println("LocalDate.now(): " + ldNow);
    //		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
    //		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
    //		assertEquals(ldNow, DataParser.decodeDate(encodedDate));
    //	}


    @Test
    fun testEncodeDateString() {
        val encodedDate = DataEncoder.encodeDate("1979-10-09")
        assertEquals("99fdcb", encodedDate.toHexString())
    }

    @Test
    fun testEncodeMaskedDate1() {
        val encodedDate = DataEncoder.encodeMaskedDate("19xx-xx-01")
        assertEquals("c3002e7c", encodedDate.toHexString())
    }

    @Test
    fun testEncodeMaskedDate2() {
        val encodedDate = DataEncoder.encodeMaskedDate("201x-04-XX")
        assertEquals("313d10da", encodedDate.toHexString())
    }

    @Test
    fun testEncodeMaskedDate3() {
        val encodedDate = DataEncoder.encodeMaskedDate("1900-xx-xx")
        assertEquals("f000076c", encodedDate.toHexString())
    }

    @Test
    fun testEncodeMaskedDate4() {
        val encodedDate = DataEncoder.encodeMaskedDate("1999-12-31")
        assertEquals("00bbddbf", encodedDate.toHexString())
    }

    @Test
    fun testEncodeMaskedDate5() {
        val encodedDate = DataEncoder.encodeMaskedDate("xxxx-xx-xx")
        assertEquals("ff000000", encodedDate.toHexString())
    }

    @Test
    //(expected = IllegalArgumentException::class)
    fun testEncodeMaskedDate6_invalidFormat() {
        assertFailsWith<IllegalArgumentException> { DataEncoder.encodeMaskedDate("19-03-2010") }

    }

    @Test
    fun testEncodeDateTime1() {
        val dateTime = LocalDateTime.parse("1957-03-25T08:15:22")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        assertEquals("02f527bf25b2", dateTimeBytes.toHexString())
    }

    @Test
    fun testEncodeDateTime2() {
        val dateTime = LocalDateTime.parse("2030-12-01T00:00:00")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        assertEquals("0aecc4c7fb80", dateTimeBytes.toHexString())
    }

    @Test
    fun testEncodeDateTime3() {
        val dateTime = LocalDateTime.parse("0001-01-01T00:00:00")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        assertEquals("00eb28c03640", dateTimeBytes.toHexString())
    }

    @Test
    fun testEncodeDateTime4() {
        val dateTime = LocalDateTime.parse("9999-12-31T23:59:59")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        assertEquals("0b34792d9777", dateTimeBytes.toHexString())
    }

    @Test
    fun testRegex() {
        val dateString = "1979-10-09"
        val formattedDate = dateString.replace("(.{4})-(.{2})-(.{2})".toRegex(), "$2$3$1")
        assertEquals("10091979", formattedDate)
    }

    @Test
    fun testEncodeDate_String() {
        val encodedDate = DataEncoder.encodeDate("2024-09-27")
        println("encodedDate: " + encodedDate.toHexString())

        assertEquals("8d7ad8", encodedDate.toHexString())
    }

    //	@Test
    //	public void testGetSignerCertRef() throws InvalidNameException, KeyStoreException {
    //		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
    //		String signerCertRef[] = DataEncoder.getSignerCertRef(cert);
    //
    //		assertEquals("DETS", signerCertRef[0]);
    //		assertEquals("32", signerCertRef[1]);
    //	}

    @Test
    fun testZip() {
        val bytesToCompress =
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()

        val compressedBytes = DataEncoder.zip(bytesToCompress)
        println("Compressed: " + compressedBytes.toHexString())
        assertEquals(
            "78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b",
            compressedBytes.toHexString()
        )
    }


    @Test
    fun testEncodeC40() {
        val c40Bytes = DataEncoder.encodeC40("DETS32")
        assertContentEquals("6d32c91f".hexToByteArray(), c40Bytes)
        val str = DataParser.decodeC40(c40Bytes)
        println(str)
        assertEquals("DETS32", str)
    }


}
