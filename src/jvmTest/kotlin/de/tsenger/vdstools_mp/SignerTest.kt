package de.tsenger.vdstools_mp


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.util.*

class SignerTest {
    @Test
    fun testKeyStoreConstructor() {
        val signer = Signer(keystore, keyStorePassword, "dets32")
        assertEquals(224, signer.fieldSize)
    }

    @Test
    fun testSign_224() {
        val signer = Signer(keystore, keyStorePassword, "dets32")
        val dataBytes = ByteArray(32)
        val rnd = Random()
        rnd.nextBytes(dataBytes)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + Hex.toHexString(signatureBytes))
        Assert.assertTrue(signatureBytes.size * 4 == signer.fieldSize)
    }

    @Test
    fun testSign_256() {
        val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
        val rnd = SecureRandom()
        keyGen.initialize(256, rnd)
        val pair = keyGen.generateKeyPair()

        val dataBytes = ByteArray(32)
        rnd.nextBytes(dataBytes)

        val signer = Signer(pair.private as ECPrivateKey)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + Hex.toHexString(signatureBytes))
        Assert.assertTrue(signatureBytes.size * 4 == signer.fieldSize)
    }

    @Test

    fun testSign_384() {
        val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
        val rnd = SecureRandom()
        keyGen.initialize(384, rnd)
        val pair = keyGen.generateKeyPair()

        val dataBytes = ByteArray(64)
        rnd.nextBytes(dataBytes)

        val signer = Signer(pair.private as ECPrivateKey)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + Hex.toHexString(signatureBytes))
        Assert.assertTrue(signatureBytes.size * 4 == signer.fieldSize)
    }

    @Test

    fun testSign_512() {
        val kpgparams = ECGenParameterSpec("brainpoolP512r1")
        val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
        keyGen.initialize(kpgparams)
        val pair = keyGen.generateKeyPair()

        val rnd = Random()
        val dataBytes = ByteArray(128)
        rnd.nextBytes(dataBytes)

        val signer = Signer(pair.private as ECPrivateKey)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + Hex.toHexString(signatureBytes))
        Assert.assertTrue(signatureBytes.size * 4 == signer.fieldSize)
    }

    @Test(expected = Exception::class)
    fun testSign_521() {
        val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
        val rnd = SecureRandom()
        keyGen.initialize(521, rnd)
        val pair = keyGen.generateKeyPair()

        val dataBytes = ByteArray(128)
        rnd.nextBytes(dataBytes)

        val signer = Signer(pair.private as ECPrivateKey)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + Hex.toHexString(signatureBytes))
        Assert.assertTrue(signatureBytes.size * 4 == signer.fieldSize)
    }

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/jvmTest/resources/vdstools_testcerts.bks"
        lateinit var keystore: KeyStore

        @JvmStatic
        @BeforeClass

        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
    }
}
