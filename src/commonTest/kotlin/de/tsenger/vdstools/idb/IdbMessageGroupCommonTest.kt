package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbMessageGroup.Companion.fromByteArray
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IdbMessageGroupCommonTest {
    @Test
    fun testConstructorEmpty() {
        val messageGroup = IdbMessageGroup(emptyList())
        assertNotNull(messageGroup)
    }

    @Test
    fun testConstructorIdbMessage() {
        val message = IdbMessage.fromNameAndRawBytes("CAN", "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(message))
        assertNotNull(messageGroup)
        assertEquals(1, messageGroup.messagesList.size.toLong())
    }


    @Test
    fun testmessagesList() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .addMessage(
                "MRZ_TD1",
                "I<URYEWCVECOXY8<<<<<<<<<<<<<<<7206122M2811062URY<<<<<<<<<<<8BUCKLEY<<WINIFRED<<<<<<<<<<<<<"
            )
            .build()
        assertEquals(2, messageGroup.messagesList.size.toLong())
        val messageList = messageGroup.messagesList
        assertEquals("CAN", messageList[0].messageTypeName)
        assertEquals("MRZ_TD1", messageList[1].messageTypeName)
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val message1 = IdbMessage.fromNameAndRawBytes("CAN", "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val message2 = IdbMessage.fromNameAndRawBytes("MRZ_TD1", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(message1, message2))
        assertEquals(
            "611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", messageGroup.encoded.toHexString()
        )
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val messageGroup =
            fromByteArray("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageList = messageGroup.messagesList
        assertEquals("CAN", messageList[0].messageTypeName)
        assertEquals("MRZ_TD1", messageList[1].messageTypeName)
    }
}
