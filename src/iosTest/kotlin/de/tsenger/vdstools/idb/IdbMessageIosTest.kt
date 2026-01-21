package de.tsenger.vdstools.idb

import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@OptIn(ExperimentalStdlibApi::class)
class MessageIosTest {

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
        assertEquals("654321", message.value.toString())
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
