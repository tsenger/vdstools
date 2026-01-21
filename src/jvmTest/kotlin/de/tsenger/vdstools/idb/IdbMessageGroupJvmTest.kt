package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.idb.IdbMessageGroup.Companion.fromByteArray
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class IdbMessageGroupJvmTest {
    @Test
    fun testConstructorEmpty() {
        val messageGroup = IdbMessageGroup(emptyList())
        Assert.assertNotNull(messageGroup)
    }

    @Test
    fun testConstructorWithDerTlv() {
        val derTlv = DerTlv(0x09, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"))
        val messageGroup = IdbMessageGroup(listOf(derTlv))
        Assert.assertNotNull(messageGroup)
        Assert.assertEquals(1, messageGroup.messageList.size.toLong())
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
        Assert.assertEquals(2, messageGroup.messageList.size.toLong())
        val messageList = messageGroup.messageList
        Assert.assertEquals("CAN", messageList[0].name)
        Assert.assertEquals("MRZ_TD1", messageList[1].name)
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val derTlv1 = DerTlv(0x09, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"))
        val derTlv2 = DerTlv(0x07, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
        val messageGroup = IdbMessageGroup(listOf(derTlv1, derTlv2))
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
        val messageList = messageGroup.messageList
        Assert.assertEquals("CAN", messageList[0].name)
        Assert.assertEquals("MRZ_TD1", messageList[1].name)
    }
}
