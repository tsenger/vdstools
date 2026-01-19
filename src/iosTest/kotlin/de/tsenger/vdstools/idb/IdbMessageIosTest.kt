package de.tsenger.vdstools.idb

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class IdbMessageIosTest {

    var visa_content: ByteArray =
        ("022cdd52134a74da1347c6fed95cb89f"
                + "9fce133c133c133c133c203833734aaf"
                + "47f0c32f1a1e20eb2625393afe310403"
                + "a00000050633be1fed20c6").hexToByteArray()

    var idbMessageBytes: ByteArray =
        ("013b022cdd52134a74da1347c6fed95c"
                + "b89f9fce133c133c133c133c20383373"
                + "4aaf47f0c32f1a1e20eb2625393afe31"
                + "0403a00000050633be1fed20c6").hexToByteArray()


    @Test
    fun testConstructor() {
        val message = IdbMessage.fromNameAndContent("VISA", visa_content)
        assertNotNull(message)
    }

    @Test
    fun testFromByteArray() {
        val message = IdbMessage.fromByteArray(idbMessageBytes)
        assertNotNull(message)
    }

    @Test
    fun testGetEncoded() {
        val message = IdbMessage.fromNameAndContent("VISA", visa_content)
        assertContentEquals(idbMessageBytes, message.encoded)
    }

    @Test
    fun testGetMessageType() {
        val message = IdbMessage.fromByteArray(idbMessageBytes)
        assertEquals("VISA", message.messageTypeName)
    }

    @Test
    fun testGetMessageContent() {
        val message = IdbMessage.fromByteArray(idbMessageBytes)
        assertContentEquals(visa_content, message.value.rawBytes)
    }
}
