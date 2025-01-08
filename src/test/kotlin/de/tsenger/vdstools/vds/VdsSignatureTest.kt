package de.tsenger.vdstools.vds

import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.security.KeyStore

class VdsSignatureTest {
    @Test
    fun testGetPlainSignatureBytes() {
        val signature = VdsSignature(plainSignature)
        Assert.assertEquals(
            "3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
            Hex.toHexString(signature.plainSignatureBytes)
        )
    }

    @Test
    fun testGetDerSignatureBytes() {
        val signature = VdsSignature(plainSignature)
        Assert.assertEquals(
            "304502203c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357022100e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
            Hex.toHexString(signature.derSignatureBytes)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val vdsSigBytes = byteArrayOf(0xff.toByte(), 6, 1, 2, 3, 4, 5, 6)
        val signature = VdsSignature.fromByteArray(vdsSigBytes)
        Assert.assertEquals("010203040506", Hex.toHexString(signature!!.plainSignatureBytes))
        Assert.assertEquals(
            "300a02030102030203040506", Hex.toHexString(
                signature.derSignatureBytes
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class)
    fun testFromByteArray_IllegalArgumentException() {
        VdsSignature.fromByteArray(plainSignature)
    }

    companion object {
        val plainSignature: ByteArray = Hex.decode(
            "3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27"
        )
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/test/resources/vdstools_testcerts.bks"
        var keystore: KeyStore? = null
    }
}
