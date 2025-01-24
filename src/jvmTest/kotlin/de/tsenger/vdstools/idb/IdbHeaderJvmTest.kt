package de.tsenger.vdstools.idb


import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class IdbHeaderJvmTest {
    @Test
    @Throws(IOException::class)
    fun testConstructor_minimal() {
        val header = IdbHeader("D<<")
        Assert.assertEquals("6abc", Hex.toHexString(header.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testConstructor_full() {
        val header = IdbHeader(
            "D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(1, 2, 3, 4, 5),
            "2014-10-18"
        )
        Assert.assertEquals("6abc010102030405009b5d7e", Hex.toHexString(header.encoded))
    }

    @Test
    fun testGetCountryIdentifier() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"))
        Assert.assertEquals("D<<", header.getCountryIdentifier())
    }

    @Test
    fun testGetSignatureAlgorithm() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"))
        Assert.assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm())
    }

    @Test
    fun testGetSignatureAlgorithm_null() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc"))
        Assert.assertNull(header.getSignatureAlgorithm())
    }

    @Test
    fun testGetCertificateReference() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"))
        Assert.assertEquals("0102030405", Hex.toHexString(header.certificateReference))
    }

    @Test
    fun testGetCertificateReference_null() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc"))
        Assert.assertNull(header.certificateReference)
    }

    @Test
    fun testGetSignatureCreationDate() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"))
        Assert.assertEquals("2014-10-18", header.getSignatureCreationDate())
    }

    @Test
    fun testGetSignatureCreationDate_null() {
        val header = IdbHeader.fromByteArray(Hex.decode("6abc"))
        Assert.assertNull(header.getSignatureCreationDate())
    }

    @Test
    fun testFromByteArray_minimal() {
        val header = IdbHeader.fromByteArray(Hex.decode("eb11"))
        Assert.assertEquals("XKC", header.getCountryIdentifier())
        Assert.assertNull(header.getSignatureAlgorithm())
        Assert.assertNull(header.certificateReference)
        Assert.assertNull(header.getSignatureCreationDate())
    }

    @Test
    fun testFromByteArray_full() {
        val header = IdbHeader.fromByteArray(Hex.decode("eb1101aabbccddee00bbddbf"))
        Assert.assertEquals("XKC", header.getCountryIdentifier())
        Assert.assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm())
        Assert.assertEquals("aabbccddee", Hex.toHexString(header.certificateReference))
        Assert.assertEquals("1999-12-31", header.getSignatureCreationDate())
    }
}
