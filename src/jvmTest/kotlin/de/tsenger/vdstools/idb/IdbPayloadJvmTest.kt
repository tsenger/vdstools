package de.tsenger.vdstools.idb

import de.tsenger.vdstools.idb.IdbPayload.Companion.fromByteArray
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.security.cert.CertificateException

class IdbPayloadJvmTest {
    @Test
    fun testConstructor_null() {
        val payload = IdbPayload(IdbHeader("UTO"), IdbMessageGroup(emptyList()), null, null)
        Assert.assertNotNull(payload)
    }

    @Test
    fun testConstructorWithoutSignature() {
        val header = IdbHeader("D<<")
        val messageGroup = IdbMessageGroup(
            listOf(IdbMessage.fromNameAndContent("PROOF_OF_RECOVERY", Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")))
        )
        val payload = IdbPayload(header, messageGroup, null, null)
        Assert.assertNotNull(payload)
    }

    @Test
    @Throws(IOException::class)
    fun testConstructorWithoutCertificate() {
        val header = IdbHeader("D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(5, 4, 3, 2, 1))
        val messageGroup = IdbMessageGroup(
            listOf(
                IdbMessage.fromNameAndContent("PROOF_OF_VACCINATION", Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"))
            )
        )
        val signature = IdbSignature(
            Hex.decode(
                "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
            )
        )
        val payload = IdbPayload(header, messageGroup, null, signature)
        Assert.assertNotNull(payload)
    }

    @Test
    @Throws(IOException::class, CertificateException::class)
    fun testGetIdbHeader() {
        val rawBytes = Hex.decode(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val payload = fromByteArray(rawBytes, true)
        val header = payload.idbHeader
        Assert.assertEquals("6abc010504030201009b5d88", Hex.toHexString(header.encoded))
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testGetIdbMessageGroup() {
        val rawBytes = Hex.decode(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val payload = fromByteArray(rawBytes, true)
        val messageGroup = payload.idbMessageGroup
        Assert.assertNotNull(messageGroup)
        Assert.assertEquals(
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", Hex.toHexString(
                messageGroup.encoded
            )
        )
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testGetIdbSignerCertificate_null() {
        val rawBytes = Hex.decode(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val payload = fromByteArray(rawBytes, true)
        val signerCert = payload.idbSignerCertificate
        Assert.assertNull(signerCert)
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testGetIdbSignature() {
        val rawBytes = Hex.decode(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val payload = fromByteArray(rawBytes, true)
        val signature = payload.idbSignature
        Assert.assertNotNull(signature)
        Assert.assertEquals(
            "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            Hex.toHexString(signature!!.plainSignatureBytes)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedWithoutSignature() {
        val header = IdbHeader("D<<")
        val messageGroup = IdbMessageGroup(
            listOf(IdbMessage.fromNameAndContent("PROOF_OF_RECOVERY", Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")))
        )
        val payload = IdbPayload(header, messageGroup, null, null)
        val encodedBytes = payload.encoded
        Assert.assertEquals("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", Hex.toHexString(encodedBytes))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncodedWithoutCertificate() {
        val header = IdbHeader(
            "D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(5, 4, 3, 2, 1),
            "2024-10-18"
        )
        val messageGroup = IdbMessageGroup(
            listOf(IdbMessage.fromNameAndContent("PROOF_OF_VACCINATION", Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")))
        )
        val signature = IdbSignature(
            Hex.decode(
                "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
            )
        )
        val payload = IdbPayload(header, messageGroup, null, signature)
        val encodedBytes = payload.encoded
        Assert.assertEquals(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            Hex.toHexString(encodedBytes)
        )
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromByteArray() {
        val rawBytes = Hex.decode(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
                    + "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val payload = fromByteArray(rawBytes, true)
        Assert.assertNotNull(payload)
    }
}
