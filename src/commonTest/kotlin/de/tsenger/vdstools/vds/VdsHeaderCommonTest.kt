package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.Buffer
import okio.EOFException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalStdlibApi::class)
class VdsHeaderCommonTest {


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

    @OptIn(ExperimentalTime::class)
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
        val expectedHeaderBytes =
            "dc036abc6d32c8a72cb1".hexToByteArray() + encodedDate + encodedDate + "fb06".hexToByteArray()
        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @OptIn(ExperimentalTime::class)
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

        val expectedHeaderBytes =
            "dc03ed586d32c8a72cb1".hexToByteArray() + encodedDate + encodedDate + "fb06".hexToByteArray()
        assertContentEquals(expectedHeaderBytes, headerBytes)
    }

    @OptIn(ExperimentalTime::class)
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
        val expectedHeaderBytes =
            "dc03ed586d32c8a72cb1".hexToByteArray() + issuingDate + signDate + "fb06".hexToByteArray()
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
        assertFailsWith<EOFException> {
            VdsHeader.fromBuffer(buffer)
        }
    }

    @Test
    fun testCertRefLengthCalculation() {
        // Test bytesToDecode calculation for certificate reference lengths 1-32
        for (length in 1..32) {
            // Create certificate reference with desired length
            val certRef = "1".repeat(length)

            // Build header with version 3 (rawVersion 0x03)
            val header = VdsHeader.Builder("RESIDENCE_PERMIT")
                .setRawVersion(3)
                .setIssuingCountry("D<<")
                .setSignerIdentifier("DETS")
                .setCertificateReference(certRef)
                .setIssuingDate(LocalDate.parse("2024-09-27"))
                .setSigDate(LocalDate.parse("2024-09-27"))
                .build()

            // Encode header to bytes
            val encoded = header.encoded

            // Parse header back from bytes
            val buffer = Buffer().write(encoded)
            val parsed = VdsHeader.fromBuffer(buffer)

            // Verify that the parsed certificate reference matches the original
            assertEquals(
                certRef,
                parsed.certificateReference,
                "Certificate reference mismatch for length $length"
            )
        }
    }

    @Test
    fun testGetEncoded_AddressStickerRP() {
        // ADDRESS_STICKER_RP 0xf90c
        val header = VdsHeader.Builder("ADDRESS_STICKER_RP")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("5022026")
            .setIssuingDate(LocalDate.parse("2026-02-05"))
            .setSigDate(LocalDate.parse("2026-02-05"))
            .build()
        val headerBytes = header.encoded

        assertEquals("dc036abc6d32c8ac38e72627fe371f4fba1f4fbaf90c", headerBytes.toHexString())
    }

    @Test
    fun testParseByteArray_AddressStickerRP() {
        val buffer = Buffer().write("dc036abc6d32c8ac38e72627fe371f4fba1f4fbaf90c".hexToByteArray())
        val header = VdsHeader.fromBuffer(buffer)
        assertEquals("ADDRESS_STICKER_RP", header.vdsType)
        assertEquals("D  ", header.issuingCountry)
        assertEquals("DETS", header.signerIdentifier)
        assertEquals("5022026", header.certificateReference)
        assertEquals("2026-02-05", header.issuingDate.toString())
        assertEquals("2026-02-05", header.sigDate.toString())
        assertEquals(0xf90c, header.documentRef)
    }

    @Test
    fun testCertRefLengthCalculation_DEZV() {
        // Test bytesToDecode calculation for DEZV with decimal certRefLength
        // This verifies the formula works correctly when radix=10
        // Note: DEZV uses decimal encoding for certRefLength (not hexadecimal)
        for (length in 1..32) {
            val certRef = "A".repeat(length) // Use 'A' for better visibility
            val lengthStr = length.toString(10).padStart(2, '0') // Decimal!

            // Manually build header bytes with DEZV and decimal length
            val buffer = Buffer()
            buffer.writeByte(0xDC)
            buffer.writeByte(0x03) // Version 4
            buffer.write(DataEncoder.encodeC40("D<<"))
            buffer.write(DataEncoder.encodeC40("DEZV$lengthStr")) // Signer + decimal length
            buffer.write(DataEncoder.encodeC40(certRef)) // Certificate reference
            buffer.write(DataEncoder.encodeDate(LocalDate.parse("2026-01-07")))
            buffer.write(DataEncoder.encodeDate(LocalDate.parse("2026-01-07")))
            buffer.writeByte(0x01) // always 1dec as defined in BSI TR-03171
            buffer.writeByte(0xC8) // always 200dec as defined in BSI TR-03171

            // Parse the manually created header
            val parseBuffer = Buffer().write(buffer.readByteArray())
            val parsed = VdsHeader.fromBuffer(parseBuffer)

            // Verify
            assertEquals(
                "DEZV",
                parsed.signerIdentifier,
                "SignerIdentifier mismatch for length $length"
            )
            assertEquals(
                certRef,
                parsed.certificateReference,
                "Certificate reference mismatch for DEZV with length $length (decimal)"
            )
        }
    }

}
