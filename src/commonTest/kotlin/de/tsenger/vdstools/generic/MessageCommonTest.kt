package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.idb.IdbMessageGroup
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
}
