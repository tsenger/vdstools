package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbPayload.Companion.fromByteArray
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalStdlibApi::class)
class IdbPayloadCommonTest {
    @Test
    fun testConstructor_null() {
        val messageGroup = IdbMessageGroup.Builder().build()
        val payload = IdbPayload(IdbHeader("UTO"), messageGroup, null, null)
        assertNotNull(payload)
    }

    @Test
    fun testConstructorWithoutSignature() {
        val header = IdbHeader("D<<")
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("PROOF_OF_RECOVERY", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()
        val payload = IdbPayload(header, messageGroup, null, null)
        assertNotNull(payload)
    }

    @Test
    @Throws(IOException::class)
    fun testConstructorWithoutCertificate() {
        val header = IdbHeader("D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(5, 4, 3, 2, 1))
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("PROOF_OF_VACCINATION", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()
        val signature = IdbSignature(
            "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        )
        val payload = IdbPayload(header, messageGroup, null, signature)
        assertNotNull(payload)
    }

    @Test
    fun testGetIdbHeader() {
        val rawBytes =
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        val payload = fromByteArray(rawBytes, true)
        val header = payload.idbHeader
        assertEquals("6abc010504030201009b5d88", header.encoded.toHexString())
    }

    @Test
    fun testGetIdbMessageGroup() {
        val rawBytes =
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        val payload = fromByteArray(rawBytes, true)
        val messageGroup = payload.idbMessageGroup
        assertNotNull(messageGroup)
        assertEquals(
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", messageGroup.encoded.toHexString()
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetIdbSignerCertificate_null() {
        val rawBytes =
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        val payload = fromByteArray(rawBytes, true)
        val signerCert = payload.idbSignerCertificate
        assertNull(signerCert)
    }

    @Test
    fun testGetIdbSignature() {
        val rawBytes =
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()

        val payload = fromByteArray(rawBytes, true)
        val signature = payload.idbSignature
        assertNotNull(signature)
        assertEquals(
            "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            signature.plainSignatureBytes.toHexString()
        )
    }

    @Test
    fun testGetEncodedWithoutSignature() {
        val header = IdbHeader("D<<")
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("PROOF_OF_RECOVERY", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()
        val payload = IdbPayload(header, messageGroup, null, null)
        val encodedBytes = payload.encoded
        assertEquals("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", encodedBytes.toHexString())
    }

    @Test
    fun testGetEncodedWithoutCertificate() {
        val header = IdbHeader(
            "D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(5, 4, 3, 2, 1),
            "2024-10-18"
        )
        val messageGroup = IdbMessageGroup.Builder()
            .addFeature("PROOF_OF_VACCINATION", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()
        val signature = IdbSignature(
            "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        )
        val payload = IdbPayload(header, messageGroup, null, signature)
        val encodedBytes = payload.encoded
        assertEquals(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            encodedBytes.toHexString()
        )
    }

    @Test
    fun testFromByteArray() {
        val rawBytes =
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()

        val payload = fromByteArray(rawBytes, true)
        assertNotNull(payload)
    }
}
