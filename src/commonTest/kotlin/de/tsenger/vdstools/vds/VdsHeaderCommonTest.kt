package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
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
        assertFails {
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

    @Test
    fun testEncode_DEZV_usesDecimalLengthForSha1CertRef() {
        // Per BSI TR-03171 a DEZV cert ref is the 20-byte SHA-1 hash rendered as a
        // 40-char hex string, and its length is encoded as the decimal value 40 → "DEZV40"
        // (hex would be "28"). Verify the encoder emits decimal and round-trips.
        val certRef = "00112233445566778899AABBCCDDEEFF00112233" // 40 chars / 20 bytes
        val header = VdsHeader.Builder("ADMINISTRATIVE_DOCUMENTS_V8")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DEZV")
            .setCertificateReference(certRef)
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()

        val encoded = header.encoded
        // bytes: magic(1) + version(1) + country C40(2) + signer&length C40(4) + ...
        val signerAndLength = DataEncoder.decodeC40(encoded.copyOfRange(4, 8))
        assertEquals("DEZV40", signerAndLength, "DEZV length must be decimal 40, not hex 28")

        // Round-trip: decoding must yield back the full 40-char cert ref
        val parsed = VdsHeader.fromBuffer(Buffer().write(encoded))
        assertEquals("DEZV", parsed.signerIdentifier)
        assertEquals(certRef, parsed.certificateReference)
    }

    @Test
    fun testParseByteArray_DEZV_Sha1CertRef_WithTrailingSealBytes() {
        // Regression: a real TR-03171 (DEZV) seal whose 40-char SHA-1 cert ref encodes its
        // length as decimal "40" ("DEZV40"). Read as hex, "40" = 0x40 = 64 -> the parser
        // over-reads 16 bytes; because trailing message/signature bytes follow the header,
        // the over-read succeeds and (here) yields coincidentally valid dates, so the old
        // radix-16-first strategy never fell back to radix 10 and silently mis-parsed the
        // seal. The header-only round-trip test above does NOT catch this, because without
        // trailing bytes the over-read underflows the buffer and triggers the fallback.
        val rawSeal = (
            "dc036abc6d38dc055f80585a34136c0b66913397209426a03acb788758f459461a74fe3860" +
                "294a60294a01c900104f2b91c7a8e5402dbc3169d7e0a45f1803127473656e6765722e64652f" +
                "70726f66696c65040f7473656e6765722e64652f6365727401083230323630363330020832303" +
                "237303633300a0653656e6765720b06546f626961730c0831393739313030390d0c4d65676143" +
                "6f7270204c74640e114175662064656d2048c3bc67656c2034370f053533333437100954303030" +
                "30303037331106059f5d790c8c120105130101ff4062f15d53ba85cf1659e171762c2a910e46a6" +
                "d1ae2c7d61d5f3be329e96ecbb0f65fcafdd4b6e48afef1250fcd98458c61e5e869176aa72d98bd" +
                "fb7a851458acc"
            ).hexToByteArray()

        val buffer = Buffer().write(rawSeal)
        val header = VdsHeader.fromBuffer(buffer)

        assertEquals("DEZV", header.signerIdentifier)
        assertEquals("B73A1D496D7ECCC46214F2335C6F7AA57A790577", header.certificateReference)
        assertEquals("2026-06-30", header.issuingDate.toString())
        assertEquals("2026-06-30", header.sigDate.toString())
        // The header is exactly 44 bytes; trailing seal bytes must remain untouched so the
        // subsequent DerTlv message/signature parsing stays aligned.
        assertEquals((rawSeal.size - 44).toLong(), buffer.size)
    }

}
