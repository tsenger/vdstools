package de.tsenger.vdstools.idb

import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class MessageJvmTest {

    @Test
    fun testMessageConstructor() {
        val bytes = Hex.decode("a0a1a2a3a4a5")
        val message = Message(0x09, "CAN", MessageCoding.UTF8_STRING, MessageValue.BytesValue(bytes))
        Assert.assertNotNull(message)
        Assert.assertEquals(0x09, message.tag)
        Assert.assertEquals("CAN", message.name)
        Assert.assertEquals(MessageCoding.UTF8_STRING, message.coding)
    }

    @Test
    fun testMessageToString() {
        val bytes = "TestValue".encodeToByteArray()
        val message = Message(0x09, "CAN", MessageCoding.UTF8_STRING, MessageValue.StringValue("TestValue", bytes))
        Assert.assertEquals("TestValue", message.toString())
    }

    @Test
    fun testMessageEncoded() {
        val bytes = Hex.decode("a0a1a2a3")
        val message = Message(0x09, "CAN", MessageCoding.BYTES, MessageValue.BytesValue(bytes))
        // DerTlv encoding: tag (0x09) + length (0x04) + value
        Assert.assertEquals("0904a0a1a2a3", Hex.toHexString(message.encoded))
    }

    @Test
    fun testMessageFromIdbMessageGroup() {
        // Create IdbMessageGroup and get Message from it
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .build()

        val message = messageGroup.getMessage("CAN")
        Assert.assertNotNull(message)
        Assert.assertEquals("CAN", message!!.name)
        Assert.assertTrue(message.value is MessageValue.StringValue)
        Assert.assertEquals("654321", message.value.toString())
    }

    @Test
    fun testMessageValueAccess() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "123456")
            .build()

        val message = messageGroup.messageList[0]
        Assert.assertNotNull(message)
        Assert.assertEquals("CAN", message.name)
        Assert.assertEquals(0x09, message.tag)
    }
}
