package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbMessageGroup.Companion.fromByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IdbMessageGroupCommonTest {
    @Test
    fun testConstructorEmpty() {
        val messageGroup = IdbMessageGroup()
        assertNotNull(messageGroup)
    }

    @Test
    fun testConstructorIdbMessage() {
        val message = IdbMessage(IdbMessageType.CAN, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val messageGroup = IdbMessageGroup(message)
        assertNotNull(messageGroup)
        assertEquals(1, messageGroup.getMessagesList().size.toLong())
    }

    @Test
    fun testAddMessage() {
        val message = IdbMessage(IdbMessageType.MRZ_TD1, "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(message)
        assertEquals(1, messageGroup.getMessagesList().size.toLong())
    }

    @Test
    fun testGetMessagesList() {
        val message = IdbMessage(IdbMessageType.CAN, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val message2 = IdbMessage(IdbMessageType.MRZ_TD1, "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup(message)
        messageGroup.addMessage(message2)
        assertEquals(2, messageGroup.getMessagesList().size.toLong())
        val messageList = messageGroup.getMessagesList()
        assertEquals(IdbMessageType.CAN, messageList[0].getMessageType())
        assertEquals(IdbMessageType.MRZ_TD1, messageList[1].getMessageType())
    }

    @Test
    fun testGetEncoded() {
        val message1 = IdbMessage(IdbMessageType.CAN, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val message2 = IdbMessage(IdbMessageType.MRZ_TD1, "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup()
        messageGroup.addMessage(message1)
        messageGroup.addMessage(message2)
        assertEquals(
            "611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
            messageGroup.encoded.toHexString()
        )
    }

    @Test
    fun testFromByteArray() {
        val messageGroup =
            fromByteArray("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageList = messageGroup.getMessagesList()
        assertEquals(IdbMessageType.CAN, messageList[0].getMessageType())
        assertEquals(IdbMessageType.MRZ_TD1, messageList[1].getMessageType())
    }
}
