package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
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
    fun testConstructorWithDerTlv() {
        val derTlv = DerTlv(0x09, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(derTlv))
        assertNotNull(messageGroup)
        assertEquals(1, messageGroup.messageList.size.toLong())
    }


    @Test
    fun testMessageList() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .addMessage(
                "MRZ_TD1",
                "I<URYEWCVECOXY8<<<<<<<<<<<<<<<7206122M2811062URY<<<<<<<<<<<8BUCKLEY<<WINIFRED<<<<<<<<<<<<<"
            )
            .build()
        assertEquals(2, messageGroup.messageList.size.toLong())
        val messageList = messageGroup.messageList
        assertEquals("CAN", messageList[0].name)
        assertEquals("MRZ_TD1", messageList[1].name)
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val derTlv1 = DerTlv(0x09, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val derTlv2 = DerTlv(0x07, "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(derTlv1, derTlv2))
        assertEquals(
            "611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", messageGroup.encoded.toHexString()
        )
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val messageGroup =
            fromByteArray("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageList = messageGroup.messageList
        assertEquals("CAN", messageList[0].name)
        assertEquals("MRZ_TD1", messageList[1].name)
    }
}
