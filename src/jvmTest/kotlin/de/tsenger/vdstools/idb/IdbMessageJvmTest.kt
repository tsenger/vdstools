package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbMessage.Companion.fromByteArray
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class IdbMessageJvmTest {
    //@formatter:off
    var visa_content: ByteArray = Hex.decode(
    ("022cdd52134a74da1347c6fed95cb89f"
    + "9fce133c133c133c133c203833734aaf"
    + "47f0c32f1a1e20eb2625393afe310403"
    + "a00000050633be1fed20c6"))
    
    var idbMessageBytes: ByteArray = Hex.decode(
    ("013b022cdd52134a74da1347c6fed95c"
    + "b89f9fce133c133c133c133c20383373"
    + "4aaf47f0c32f1a1e20eb2625393afe31"
    + "0403a00000050633be1fed20c6"))
    
     //@formatter:on
     @Test
     @Throws(IOException::class)
     fun testConstructor() {
         val message = IdbMessage("VISA", visa_content)
         println(Hex.toHexString(message.encoded))
         Assert.assertNotNull(message)
     }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val message = fromByteArray(idbMessageBytes)
        Assert.assertNotNull(message)
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val message = IdbMessage("VISA", visa_content)
        Assert.assertArrayEquals(idbMessageBytes, message.encoded)
    }

    @Test
    @Throws(IOException::class)
    fun testGetMessageType() {
        val message = fromByteArray(idbMessageBytes)
        Assert.assertEquals("VISA", message.messageTypeName)
    }

    @Test
    @Throws(IOException::class)
    fun testGetMessageContent() {
        val message = fromByteArray(idbMessageBytes)
        Assert.assertArrayEquals(visa_content, message.messageContent)
    }
}
