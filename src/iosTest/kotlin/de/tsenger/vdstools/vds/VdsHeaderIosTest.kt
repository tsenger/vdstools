package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.Buffer
import okio.EOFException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class VdsHeaderIosTest {
    @Test
    fun testGetDocumentRef() {
        val header = VdsHeader.Builder("ALIENS_LAW").build()
        assertEquals(header.documentRef.toLong(), 0x01fe)
    }


    @Test
    fun testGetEncoded_V3() {
        // RESIDENCE_PERMIT 0xfb06
        val header = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val headerBytes = header.encoded

        assertEquals("dc036abc6d32c8a72cb18d7ad88d7ad8fb06", headerBytes.toHexString())
    }

    @Test
    fun testGetEncoded_V2() {
        // RESIDENCE_PERMIT 0xfb06
        val header = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setRawVersion(2)
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val headerBytes = header.encoded

        assertEquals("dc026abc6d32c8a51a1f8d7ad88d7ad8fb06", headerBytes.toHexString())
    }

    @Test
    fun testBuildHeader_2parameter() {
        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()
        val headerBytes = vdsHeader.encoded
        val expectedHeaderBytes = (
                "dc036abc6d32c8a72cb1".hexToByteArray()
                        + encodedDate
                        + encodedDate
                        + "fb06".hexToByteArray()
                )
        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @Test
    fun testBuildHeader_3parameter() {
        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .build()
        val headerBytes = vdsHeader.encoded

        val expectedHeaderBytes = (
                "dc03ed586d32c8a72cb1".hexToByteArray()
                        + encodedDate
                        + encodedDate
                        + "fb06".hexToByteArray()
                )
        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @Test
    fun testBuildHeader_4parameter() {
        val ldate = LocalDate.parse("2016-08-16")
        val issuingDate: ByteArray = DataEncoder.encodeDate(ldate)

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val signDate: ByteArray = DataEncoder.encodeDate(ldNow)


        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .setRawVersion(3)
            .setIssuingDate(ldate)
            .build()

        val headerBytes = vdsHeader.encoded

        val expectedHeaderBytes = (
                "dc03ed586d32c8a72cb1".hexToByteArray()
                        + issuingDate
                        + signDate
                        + "fb06".hexToByteArray()
                )
        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBuildHeader_4parameterV2() {
        val ldate = LocalDate.parse("2016-08-16")
        val issuingDate: ByteArray = DataEncoder.encodeDate(ldate)

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val signDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("TEMP_PASSPORT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .setRawVersion(2)
            .setIssuingDate(ldate)
            .build()
        val headerBytes = vdsHeader.encoded
        val expectedHeaderBytes = (
                "dc02ed586d32c8a51a1f".hexToByteArray()
                        + issuingDate
                        + signDate
                        + "f60d".hexToByteArray()
                )

        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @Test
    fun testParseByteArray_V3() {
        val buffer = Buffer().write("dc036abc6d32c8a72cb18d7ad88d7ad8fb06".hexToByteArray())
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals("RESIDENCE_PERMIT", header.vdsType)
        assertEquals("D  ", header.issuingCountry)
        assertEquals("DETS", header.signerIdentifier)
        assertEquals("32", header.certificateReference)
        assertEquals("2024-09-27", header.issuingDate.toString())
    }

    @Test
    fun testParseByteArray_V2() {
        val buffer = Buffer().write("DC02D9C56D32C8A519FC0F71346F1D67FC04".hexToByteArray())
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals("SOCIAL_INSURANCE_CARD", header.vdsType)
        assertEquals("UTO", header.issuingCountry)
        assertEquals("DETS", header.signerIdentifier)
        assertEquals("00027", header.certificateReference)
        assertEquals("2020-01-01", header.issuingDate.toString())
    }


    @Test
    fun testParseByteArray_3() {
        val buffer = Buffer().write("dc03d9cac8a73a99105b99105b99fb06".hexToByteArray())
        assertFailsWith<EOFException> { VdsHeader.fromBuffer(buffer) }
    }


}
