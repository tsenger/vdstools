package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.idb.IdbMessageGroup
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class MessageCommonTest {

    @Test
    fun testMessage_BYTE_value() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val message = Message(127, "MESSAGE1", MessageCoding.BYTE, MessageValue.fromBytes(bytes, MessageCoding.BYTE))
        assertTrue(message.value is MessageValue.ByteValue)
        assertEquals(127, message.value.value)
        assertEquals("127", message.value.toString())
    }

    @Test
    fun testMessage_C40_value() {
        val bytes = DataEncoder.encodeC40("DETS32")
        val message = Message(2, "MESSAGE2", MessageCoding.C40, MessageValue.fromBytes(bytes, MessageCoding.C40))
        assertTrue(message.value is MessageValue.StringValue)
        assertEquals("DETS32", message.value.value)
        assertEquals("DETS32", message.value.toString())
        assertEquals("DETS32", message.toString())
    }

    @Test
    fun testMessage_UTF8_value() {
        val bytes = "Jâcob".encodeToByteArray()
        val message = Message(
            3,
            "MESSAGE3",
            MessageCoding.UTF8_STRING,
            MessageValue.fromBytes(bytes, MessageCoding.UTF8_STRING)
        )
        assertTrue(message.value is MessageValue.StringValue)
        assertEquals("Jâcob", message.toString())
    }

    @Test
    fun testMessage_BYTES_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = Message(4, "MESSAGE4", MessageCoding.BYTES, MessageValue.fromBytes(bytes, MessageCoding.BYTES))
        assertTrue(message.value is MessageValue.BytesValue)
        assertEquals("badc0ffe", message.value.toString())
    }

    @Test
    fun testMessage_BYTE_valueStr() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val message = Message(5, "MESSAGE5", MessageCoding.BYTE, MessageValue.fromBytes(bytes, MessageCoding.BYTE))
        assertEquals("127", message.value.toString())
    }

    @Test
    fun testMessage_BYTES_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = Message(6, "MESSAGE6", MessageCoding.BYTES, MessageValue.fromBytes(bytes, MessageCoding.BYTES))
        assertContentEquals("BADC0FFE".hexToByteArray(), message.value.rawBytes)
    }

    @Test
    fun testMessage_UNKNOWN_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message =
            Message(6, "MESSAGE6", MessageCoding.UNKNOWN, MessageValue.fromBytes(bytes, MessageCoding.UNKNOWN))
        assertContentEquals("BADC0FFE".hexToByteArray(), message.value.rawBytes)
    }

    @Test
    fun testMessage_UNKNOWN_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message =
            Message(6, "MESSAGE6", MessageCoding.UNKNOWN, MessageValue.fromBytes(bytes, MessageCoding.UNKNOWN))
        assertEquals("badc0ffe", message.value.toString())
    }

    @Test
    fun testMessage_toString() {
        val bytes = "Test".encodeToByteArray()
        val message = Message(
            1,
            "MY_MESSAGE",
            MessageCoding.UTF8_STRING,
            MessageValue.fromBytes(bytes, MessageCoding.UTF8_STRING)
        )
        assertEquals("Test", message.toString())
    }

    @Test
    fun testMessageConstructor() {
        val bytes = "a0a1a2a3a4a5".hexToByteArray()
        val message = Message(0x09, "CAN", MessageCoding.UTF8_STRING, MessageValue.BytesValue(bytes))
        assertNotNull(message)
        assertEquals(0x09, message.tag)
        assertEquals("CAN", message.name)
        assertEquals(MessageCoding.UTF8_STRING, message.coding)
    }

    @Test
    fun testMessageToString() {
        val bytes = "TestValue".encodeToByteArray()
        val message = Message(0x09, "CAN", MessageCoding.UTF8_STRING, MessageValue.StringValue("TestValue", bytes))
        assertEquals("TestValue", message.toString())
    }

    @Test
    fun testMessageEncoded() {
        val bytes = "a0a1a2a3".hexToByteArray()
        val message = Message(0x09, "CAN", MessageCoding.BYTES, MessageValue.BytesValue(bytes))
        // DerTlv encoding: tag (0x09) + length (0x04) + value
        assertEquals("0904a0a1a2a3", message.encoded.toHexString())
    }

    @Test
    fun testMessageFromIdbMessageGroup() {
        // Create IdbMessageGroup and get Message from it
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .build()

        val message = messageGroup.getMessage("CAN")
        assertNotNull(message)
        assertEquals("CAN", message.name)
        assertTrue(message.value is MessageValue.StringValue)
        assertEquals("654321", message.toString())
    }

    @Test
    fun testMessageValueAccess() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "123456")
            .build()

        val message = messageGroup.messageList[0]
        assertNotNull(message)
        assertEquals("CAN", message.name)
        assertEquals(0x09, message.tag)
    }

    @Test
    fun testValidityDatesValue_bothDates() {
        val bytes = "20250101\u000020261231".encodeToByteArray()
        println("Raw value: ${bytes.toHexString()}")
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES)
        assertTrue(value is MessageValue.ValidityDatesValue)
        assertEquals(LocalDate(2025, 1, 1), value.validFrom)
        assertEquals(LocalDate(2026, 12, 31), value.validTo)
        assertEquals("2025-01-01 - 2026-12-31", value.toString())
    }

    @Test
    fun testValidityDatesValue_onlyValidFrom() {
        val bytes = "20250101\u0000".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES)
        assertTrue(value is MessageValue.ValidityDatesValue)
        assertEquals(LocalDate(2025, 1, 1), value.validFrom)
        assertNull(value.validTo)
        assertEquals("2025-01-01", value.toString())
    }

    @Test
    fun testValidityDatesValue_onlyValidTo() {
        val bytes = "\u000020261231".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES)
        assertTrue(value is MessageValue.ValidityDatesValue)
        assertNull(value.validFrom)
        assertEquals(LocalDate(2026, 12, 31), value.validTo)
        assertEquals("2026-12-31", value.toString())
    }

    @Test
    fun testValidityDatesValue_noDates() {
        val bytes = "\u0000".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES)
        assertTrue(value is MessageValue.ValidityDatesValue)
        assertNull(value.validFrom)
        assertNull(value.validTo)
        assertEquals("", value.toString())
    }

    @Test
    fun testValidityDatesValue_bothDates_rawBytes() {
        val bytes = "20250101\u000020261231".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES) as MessageValue.ValidityDatesValue
        // 17 Bytes: 8 (validFrom) + 1 (NUL) + 8 (validTo)
        assertEquals(17, value.rawBytes.size)
        assertContentEquals(bytes, value.rawBytes)
        assertEquals("3230323530313031003230323631323331", value.rawBytes.toHexString())
    }

    @Test
    fun testValidityDatesValue_onlyValidFrom_rawBytes() {
        val bytes = "20250101\u0000".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES) as MessageValue.ValidityDatesValue
        // 9 Bytes: 8 (validFrom) + 1 (NUL)
        assertEquals(9, value.rawBytes.size)
        assertContentEquals(bytes, value.rawBytes)
        assertEquals("323032353031303100", value.rawBytes.toHexString())
    }

    @Test
    fun testValidityDatesValue_onlyValidTo_rawBytes() {
        val bytes = "\u000020261231".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES) as MessageValue.ValidityDatesValue
        // 9 Bytes: 1 (NUL) + 8 (validTo)
        assertEquals(9, value.rawBytes.size)
        assertContentEquals(bytes, value.rawBytes)
        assertEquals("003230323631323331", value.rawBytes.toHexString())
    }

    @Test
    fun testValidityDatesValue_noDates_rawBytes() {
        val bytes = "\u0000".encodeToByteArray()
        val value = MessageValue.fromBytes(bytes, MessageCoding.VALIDITY_DATES) as MessageValue.ValidityDatesValue
        // 1 Byte: nur NUL
        assertEquals(1, value.rawBytes.size)
        assertContentEquals(bytes, value.rawBytes)
        assertEquals("00", value.rawBytes.toHexString())
    }

    @Test
    fun testValidityDatesValue_of_bothDates() {
        val value = MessageValue.ValidityDatesValue.of(
            LocalDate(2025, 1, 1), LocalDate(2026, 12, 31)
        )
        assertEquals(LocalDate(2025, 1, 1), value.validFrom)
        assertEquals(LocalDate(2026, 12, 31), value.validTo)
        assertEquals(17, value.rawBytes.size)
        assertContentEquals("20250101\u000020261231".encodeToByteArray(), value.rawBytes)
    }

    @Test
    fun testValidityDatesValue_of_onlyValidFrom() {
        val value = MessageValue.ValidityDatesValue.of(
            LocalDate(2025, 3, 15), null
        )
        assertEquals(LocalDate(2025, 3, 15), value.validFrom)
        assertNull(value.validTo)
        assertEquals(9, value.rawBytes.size)
        assertContentEquals("20250315\u0000".encodeToByteArray(), value.rawBytes)
    }

    @Test
    fun testValidityDatesValue_of_onlyValidTo() {
        val value = MessageValue.ValidityDatesValue.of(
            null, LocalDate(2026, 6, 30)
        )
        assertNull(value.validFrom)
        assertEquals(LocalDate(2026, 6, 30), value.validTo)
        assertEquals(9, value.rawBytes.size)
        assertContentEquals("\u000020260630".encodeToByteArray(), value.rawBytes)
    }

    @Test
    fun testValidityDatesValue_of_noDates() {
        val value = MessageValue.ValidityDatesValue.of(null, null)
        assertNull(value.validFrom)
        assertNull(value.validTo)
        assertEquals(1, value.rawBytes.size)
        assertContentEquals("\u0000".encodeToByteArray(), value.rawBytes)
    }

    @Test
    fun testValidityDatesValue_of_roundtrip() {
        val original = MessageValue.ValidityDatesValue.of(
            LocalDate(2025, 1, 1), LocalDate(2026, 12, 31)
        )
        val parsed = MessageValue.ValidityDatesValue.parse(original.rawBytes)
        assertEquals(original.validFrom, parsed.validFrom)
        assertEquals(original.validTo, parsed.validTo)
        assertContentEquals(original.rawBytes, parsed.rawBytes)
    }
}
