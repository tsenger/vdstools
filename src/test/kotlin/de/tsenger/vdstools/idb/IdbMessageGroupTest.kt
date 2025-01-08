package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbMessageGroup.Companion.fromByteArray
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class IdbMessageGroupTest {
    @Test
    fun testConstructorEmpty() {
        val messageGroup = IdbMessageGroup()
        Assert.assertNotNull(messageGroup)
    }

    @Test
    fun testConstructorIdbMessage() {
        val message = IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"))
        val messageGroup = IdbMessageGroup(message)
        Assert.assertNotNull(messageGroup)
        Assert.assertEquals(1, messageGroup.getMessagesList().size.toLong())
    }

    @Test
    fun testAddMessage() {
        val message = IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(message)
        Assert.assertEquals(1, messageGroup.getMessagesList().size.toLong())
    }

    @Test
    fun testGetMessagesList() {
        val message = IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"))
        val message2 = IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
        val messageGroup = IdbMessageGroup(message)
        messageGroup.addMessage(message2)
        Assert.assertEquals(2, messageGroup.getMessagesList().size.toLong())
        val messageList = messageGroup.getMessagesList()
        Assert.assertEquals(IdbMessageType.CAN, messageList[0].getMessageType())
        Assert.assertEquals(IdbMessageType.MRZ_TD1, messageList[1].getMessageType())
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val message1 = IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"))
        val message2 = IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(message1)
        messageGroup.addMessage(message2)
        Assert.assertEquals(
            "611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
            Hex.toHexString(messageGroup.encoded)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val messageGroup =
            fromByteArray(Hex.decode("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
        val messageList = messageGroup.getMessagesList()
        Assert.assertEquals(IdbMessageType.CAN, messageList[0].getMessageType())
        Assert.assertEquals(IdbMessageType.MRZ_TD1, messageList[1].getMessageType())
    }
}
