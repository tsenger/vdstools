package de.tsenger.vdstools


import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.DER
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class SignerCommonTest {


    @Test
    fun testSign_256() {
        val provider = CryptographyProvider.Default
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(EC.Curve.P256)
        val pair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = CryptographyRandom.nextBytes(32)

        val signer = Signer(pair.privateKey.encodeToByteArrayBlocking(DER), "prime256v1") //same as secp256r1
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }

    @Test
    fun testSign_384() {
        val provider = CryptographyProvider.Default
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(EC.Curve.P384)
        val pair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        val dataBytes = CryptographyRandom.nextBytes(64)

        val signer = Signer(pair.privateKey.encodeToByteArrayBlocking(DER), "secp384r1")
        val signatureBytes: ByteArray = signer.sign(dataBytes)
        println("Signature: " + signatureBytes.toHexString())
        assertEquals(signatureBytes.size * 4, signer.fieldSize)
    }

    @Test
    //(expected = Exception::class)
    fun testSign_521() {
        val provider = CryptographyProvider.Default
        val ecdsa = provider.get(ECDSA)
        val keyPairGenerator = ecdsa.keyPairGenerator(EC.Curve.P521)
        val pair: ECDSA.KeyPair = keyPairGenerator.generateKeyBlocking()

        assertFailsWith<IllegalArgumentException> {
            Signer(
                pair.privateKey.encodeToByteArrayBlocking(DER),
                "secp521r1"
            )
        }
    }


}
