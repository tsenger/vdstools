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

    @Test
    fun testFromBytes_INTEGER_decodesCorrectly() {
        val value = MessageValue.fromBytes("0190".hexToByteArray(), MessageCoding.INTEGER)
        assertIs<MessageValue.IntegerValue>(value)
        assertEquals(400L, value.value)
        assertEquals("400", value.toString())
    }

    @Test
    fun testFromBytes_INTEGER_roundTrip() {
        val bytes = DataEncoder.encodeInteger(400L)
        val value = MessageValue.fromBytes(bytes, MessageCoding.INTEGER) as MessageValue.IntegerValue
        assertEquals(400L, value.value)
        assertContentEquals(bytes, value.rawBytes)
    }

    @Test
    fun testFromBytes_BOOLEAN_decodesCorrectly() {
        val value = MessageValue.fromBytes("ff".hexToByteArray(), MessageCoding.BOOLEAN)
        assertIs<MessageValue.BooleanValue>(value)
        assertTrue(value.value)
        assertEquals("true", value.toString())
        assertFalse((MessageValue.fromBytes("00".hexToByteArray(), MessageCoding.BOOLEAN) as MessageValue.BooleanValue).value)
    }

    @Test
    fun testFromBytes_BOOLEAN_wrongLengthFallsBackToBytes() {
        assertIs<MessageValue.BytesValue>(MessageValue.fromBytes("ff00".hexToByteArray(), MessageCoding.BOOLEAN))
    }

    // -------------------------------------------------------------------------
    // DURATION_OF_STAY — ICAO 9303-13 visa, 3 unsigned bytes days/months/years
    // -------------------------------------------------------------------------

    private fun duration(hex: String) =
        assertIs<MessageValue.DurationOfStayValue>(MessageValue.fromBytes(hex.hexToByteArray(), MessageCoding.DURATION_OF_STAY))

    @Test
    fun testFromBytes_DURATION_OF_STAY_untilValidTo() {
        val v = duration("000000")
        assertEquals(MessageValue.DurationOfStayValue.Kind.UNTIL_VALID_TO, v.kind)
        assertEquals("UNTIL_VALID_TO", v.toString())
        assertEquals("000000", v.rawBytes.toHexString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_determinedAtEntry() {
        val v = duration("ffffff")
        assertEquals(MessageValue.DurationOfStayValue.Kind.DETERMINED_AT_ENTRY, v.kind)
        assertEquals("DETERMINED_AT_ENTRY", v.toString())
        assertEquals("ffffff", v.rawBytes.toHexString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_transitWithoutEntry() {
        val v = duration("fefefe")
        assertEquals(MessageValue.DurationOfStayValue.Kind.TRANSIT_WITHOUT_ENTRY, v.kind)
        assertEquals("TRANSIT_WITHOUT_ENTRY", v.toString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_daysOnly() {
        val v = duration("1e0000")
        assertEquals(MessageValue.DurationOfStayValue.Kind.DURATION, v.kind)
        assertEquals(30, v.days)
        assertEquals(0, v.months)
        assertEquals(0, v.years)
        assertEquals("30d", v.toString())
        assertEquals("30d", v.decoded)
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_mixed() {
        val v = duration("0f0601")
        assertEquals(15, v.days)
        assertEquals(6, v.months)
        assertEquals(1, v.years)
        assertEquals("1y 6m 15d", v.toString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_unsignedBytes() {
        val v = duration("a00010")
        assertEquals(MessageValue.DurationOfStayValue.Kind.DURATION, v.kind)
        assertEquals(160, v.days)
        assertEquals(0, v.months)
        assertEquals(16, v.years)
        assertEquals("16y 160d", v.toString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_partialReservedPatternIsDuration() {
        val v = duration("ff0000")
        assertEquals(MessageValue.DurationOfStayValue.Kind.DURATION, v.kind)
        assertEquals(255, v.days)
        assertEquals(0, v.months)
        assertEquals(0, v.years)
        assertEquals("255d", v.toString())
    }

    @Test
    fun testFromBytes_DURATION_OF_STAY_wrongLengthFallsBackToBytes() {
        val short = MessageValue.fromBytes("1e00".hexToByteArray(), MessageCoding.DURATION_OF_STAY)
        assertIs<MessageValue.BytesValue>(short)
        assertEquals("1e00", short.toString())
        val long = MessageValue.fromBytes("1e000000".hexToByteArray(), MessageCoding.DURATION_OF_STAY)
        assertIs<MessageValue.BytesValue>(long)
        assertEquals("1e000000", long.toString())
        assertIs<MessageValue.BytesValue>(MessageValue.fromBytes(ByteArray(0), MessageCoding.DURATION_OF_STAY))
    }

    @Test
    fun testDurationOfStayValue_ofRoundTrip() {
        val v = MessageValue.DurationOfStayValue.of(days = 15, months = 6, years = 1)
        assertEquals("0f0601", v.rawBytes.toHexString())
        assertEquals(v, duration("0f0601"))
        assertEquals("fefefe", MessageValue.DurationOfStayValue.of(MessageValue.DurationOfStayValue.Kind.TRANSIT_WITHOUT_ENTRY).rawBytes.toHexString())
        assertFailsWith<IllegalArgumentException> { MessageValue.DurationOfStayValue.of(255, 255, 255) }
        assertFailsWith<IllegalArgumentException> { MessageValue.DurationOfStayValue.of(256, 0, 0) }
    }

    @Test
    fun testEncodeValueByCoding_DURATION_OF_STAY() {
        val v = MessageValue.DurationOfStayValue.of(days = 30, months = 0, years = 0)
        assertEquals("1e0000", DataEncoder.encodeValueByCoding(MessageCoding.DURATION_OF_STAY, v).toHexString())
        assertEquals("a00000", DataEncoder.encodeValueByCoding(MessageCoding.DURATION_OF_STAY, "a00000".hexToByteArray()).toHexString())
    }
}
