package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Tests for [MessageValue.fromBytes] covering all [MessageCoding] variants,
 * with emphasis on [MessageCoding.DATE_STRING] introduced in BSI TR-03171 v0.9.
 */
@OptIn(ExperimentalStdlibApi::class)
class MessageValueCommonTest {

    // -------------------------------------------------------------------------
    // DATE — 3-byte ICAO binary format (existing, unchanged)
    // -------------------------------------------------------------------------

    @Test
    fun testFromBytes_DATE_decodesCorrectly() {
        val bytes = DataEncoder.encodeDate(LocalDate(1979, 10, 9))
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE)
        assertTrue(value is MessageValue.DateValue)
        assertEquals(LocalDate(1979, 10, 9), value.date)
    }

    @Test
    fun testFromBytes_DATE_rawBytesPreserved() {
        val bytes = DataEncoder.encodeDate(LocalDate(2024, 9, 27))
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE) as MessageValue.DateValue
        assertContentEquals(bytes, value.rawBytes)
    }

    // -------------------------------------------------------------------------
    // DATE_STRING — 8-byte UTF-8 YYYYMMDD (BSI TR-03171 v0.9)
    // -------------------------------------------------------------------------

    @Test
    fun testFromBytes_DATE_STRING_decodesCorrectly() {
        // "20250101" as UTF-8
        val bytes = "3230323530313031".hexToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE_STRING)
        assertTrue(value is MessageValue.DateValue)
        assertEquals(LocalDate(2025, 1, 1), value.date)
    }

    @Test
    fun testFromBytes_DATE_STRING_rawBytesPreserved() {
        val bytes = DataEncoder.encodeDateString(LocalDate(2025, 6, 22))
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE_STRING) as MessageValue.DateValue
        assertContentEquals(bytes, value.rawBytes)
    }

    @Test
    fun testFromBytes_DATE_STRING_roundTrip() {
        val original = LocalDate(2030, 12, 31)
        val bytes    = DataEncoder.encodeDateString(original)
        val value    = MessageValue.fromBytes(bytes, MessageCoding.DATE_STRING) as MessageValue.DateValue
        assertEquals(original, value.date)
    }

    @Test
    fun testFromBytes_DATE_STRING_toStringIsIso8601() {
        // DateValue.toString() delegates to LocalDate.toString() which produces yyyy-MM-dd
        val bytes = DataEncoder.encodeDateString(LocalDate(2025, 1, 1))
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE_STRING)
        assertEquals("2025-01-01", value.toString())
    }

    @Test
    fun testFromBytes_DATE_STRING_decodedPropertyIsLocalDate() {
        val bytes = DataEncoder.encodeDateString(LocalDate(1999, 12, 31))
        val value = MessageValue.fromBytes(bytes, MessageCoding.DATE_STRING) as MessageValue.DateValue
        assertTrue(value.decoded is LocalDate)
        assertEquals(LocalDate(1999, 12, 31), value.decoded)
    }

    @Test
    fun testFromBytes_DATE_STRING_fallsBackToBytesValueOnInvalidInput() {
        // 6 bytes is not a valid DATE_STRING; fromBytes must not throw but return BytesValue
        val badBytes = "303030303030".hexToByteArray()
        val value    = MessageValue.fromBytes(badBytes, MessageCoding.DATE_STRING)
        assertTrue(value is MessageValue.BytesValue, "Expected BytesValue fallback for invalid DATE_STRING, got ${value::class.simpleName}")
    }

    @Test
    fun testFromBytes_DATE_STRING_fallsBackToBytesValueOnNonDigits() {
        // "2025-101" contains a dash — invalid
        val badBytes = "20252d313031".hexToByteArray()
        val value    = MessageValue.fromBytes(badBytes, MessageCoding.DATE_STRING)
        assertTrue(value is MessageValue.BytesValue, "Expected BytesValue fallback for non-digit DATE_STRING")
    }

    // -------------------------------------------------------------------------
    // DATE vs DATE_STRING — verify they are distinct encodings of the same date
    // -------------------------------------------------------------------------

    @Test
    fun testDateString_rawBytesAreDifferentFromIcaoDate() {
        val date = LocalDate(2025, 1, 1)
        val dateStringBytes = DataEncoder.encodeDateString(date)
        val icaoDateBytes   = DataEncoder.encodeDate(date)
        assertFalse(dateStringBytes.contentEquals(icaoDateBytes),
            "DATE_STRING and DATE must produce different raw bytes")
    }

    @Test
    fun testDateString_decodedDateMatchesIcaoDateDecoded() {
        // Both encodings represent the same calendar date — decoded result must be equal
        val date = LocalDate(2025, 1, 1)
        val fromDateString = MessageValue.fromBytes(DataEncoder.encodeDateString(date), MessageCoding.DATE_STRING) as MessageValue.DateValue
        val fromDate       = MessageValue.fromBytes(DataEncoder.encodeDate(date),       MessageCoding.DATE)       as MessageValue.DateValue
        assertEquals(fromDate.date, fromDateString.date)
    }
}
