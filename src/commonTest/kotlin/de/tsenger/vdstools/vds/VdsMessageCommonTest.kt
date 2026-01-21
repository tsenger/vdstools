package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class VdsMessageCommonTest {

    @Test
    fun testVdsMessage_BYTE_value() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val message = VdsMessage(127, "MESSAGE1", MessageCoding.BYTE, MessageValue.fromBytes(bytes, MessageCoding.BYTE))
        assertTrue(message.value is MessageValue.ByteValue)
        assertEquals(127, (message.value as MessageValue.ByteValue).value)
        assertEquals("127", message.value.toString())
    }

    @Test
    fun testVdsMessage_C40_value() {
        val bytes = DataEncoder.encodeC40("DETS32")
        val message = VdsMessage(2, "MESSAGE2", MessageCoding.C40, MessageValue.fromBytes(bytes, MessageCoding.C40))
        assertTrue(message.value is MessageValue.StringValue)
        assertEquals("DETS32", (message.value as MessageValue.StringValue).value)
        assertEquals("DETS32", message.value.toString())
    }

    @Test
    fun testVdsMessage_UTF8_value() {
        val bytes = "Jâcob".encodeToByteArray()
        val message = VdsMessage(3, "MESSAGE3", MessageCoding.UTF8_STRING, MessageValue.fromBytes(bytes, MessageCoding.UTF8_STRING))
        assertTrue(message.value is MessageValue.StringValue)
        assertEquals("Jâcob", (message.value as MessageValue.StringValue).value)
    }

    @Test
    fun testVdsMessage_BYTES_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = VdsMessage(4, "MESSAGE4", MessageCoding.BYTES, MessageValue.fromBytes(bytes, MessageCoding.BYTES))
        assertTrue(message.value is MessageValue.BytesValue)
        assertEquals("badc0ffe", message.value.toString())
    }

    @Test
    fun testVdsMessage_BYTE_valueStr() {
        val bytes = byteArrayOf(Byte.MAX_VALUE)
        val message = VdsMessage(5, "MESSAGE5", MessageCoding.BYTE, MessageValue.fromBytes(bytes, MessageCoding.BYTE))
        assertEquals("127", message.value.toString())
    }

    @Test
    fun testVdsMessage_BYTES_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = VdsMessage(6, "MESSAGE6", MessageCoding.BYTES, MessageValue.fromBytes(bytes, MessageCoding.BYTES))
        assertContentEquals("BADC0FFE".hexToByteArray(), message.value.rawBytes)
    }

    @Test
    fun testVdsMessage_UNKNOWN_rawBytes() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = VdsMessage(6, "MESSAGE6", MessageCoding.UNKNOWN, MessageValue.fromBytes(bytes, MessageCoding.UNKNOWN))
        assertContentEquals("BADC0FFE".hexToByteArray(), message.value.rawBytes)
    }

    @Test
    fun testVdsMessage_UNKNOWN_valueStr() {
        val bytes = "BADC0FFE".hexToByteArray()
        val message = VdsMessage(6, "MESSAGE6", MessageCoding.UNKNOWN, MessageValue.fromBytes(bytes, MessageCoding.UNKNOWN))
        assertEquals("badc0ffe", message.value.toString())
    }

    @Test
    fun testVdsMessage_toString() {
        val bytes = "Test".encodeToByteArray()
        val message = VdsMessage(1, "MY_MESSAGE", MessageCoding.UTF8_STRING, MessageValue.fromBytes(bytes, MessageCoding.UTF8_STRING))
        assertEquals("MY_MESSAGE: Test", message.toString())
    }
}
