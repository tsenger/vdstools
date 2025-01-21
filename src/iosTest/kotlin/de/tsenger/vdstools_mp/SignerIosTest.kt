package de.tsenger.vdstools_mp


import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.ECDSA
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


@OptIn(ExperimentalStdlibApi::class)

class SignerIosTest {
    @Test
    fun testKeyStoreConstructor() {
        val keyPath = NSBundle.mainBundle.pathForResource("dets32", "privkey")?.toPath()
        val fileSystem = FileSystem.SYSTEM
        val privKeyBytes = fileSystem.read(keyPath!!) {
            readByteArray()
        }
        val signer = Signer(privKeyBytes, "brainpoolP224r1")
        assertEquals(224, signer.fieldSize)
    }


    @Test
    fun testSign_224() {
        val keyPath = NSBundle.mainBundle.pathForResource("dets32", "privkey")?.toPath()
        val fileSystem = FileSystem.SYSTEM
        val privKeyBytes = fileSystem.read(keyPath!!) {
            readByteArray()
        }
        val signer = Signer(privKeyBytes, "brainpoolP224r1")
        val dataBytes = ByteArray(20)
        Random(5).nextBytes(dataBytes)
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }


    @Test
    fun testSign_256() {
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(Curve.P256)

        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = ByteArray(32)
        Random(5).nextBytes(dataBytes)

        val signer = Signer(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "prime256v1")
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }

    @Test

    fun testSign_384() {
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(Curve.P384)

        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = ByteArray(64)
        Random(5).nextBytes(dataBytes)

        val signer = Signer(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "secp384r1")
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }

    @Test

    fun testSign_512() {
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(Curve("brainpoolP512r1"))

        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = ByteArray(128)
        Random(5).nextBytes(dataBytes)

        val signer = Signer(keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER), "brainpoolP512r1")
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }

    @Test
    fun testSign_521() {
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(Curve.P521)

        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = ByteArray(128)
        Random(5).nextBytes(dataBytes)

        assertFailsWith<IllegalArgumentException> {
            Signer(
                keyPair.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER),
                "secp521r1"
            )
        }

    }

}
