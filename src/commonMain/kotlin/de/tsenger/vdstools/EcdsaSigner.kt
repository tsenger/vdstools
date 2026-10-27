package de.tsenger.vdstools

import de.tsenger.vdstools.internal.ecFieldBitSize
import de.tsenger.vdstools.internal.logE
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.DER
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.RAW


class EcdsaSigner(privKeyBytes: ByteArray, curveName: String) : Signer {
    private val tag = this::class.simpleName ?: ""
    private var ecPrivKey: ECDSA.PrivateKey
    override var fieldSize: Int = 0
        private set

    init {
        // getting platform specific provider
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.privateKeyDecoder(Curve(curveName))
        ecPrivKey = keyDecoder.decodeFromByteArrayBlocking(DER, privKeyBytes)
        val keyLength = ecPrivKey.encodeToByteArrayBlocking(RAW).size
        fieldSize = ecFieldBitSize(keyLength) ?: run {
            logE(tag, "No supported curve for private key length: $keyLength bytes")
            throw IllegalArgumentException("No supported curve for private key length: $keyLength bytes")
        }
    }

    override fun sign(data: ByteArray): ByteArray {
        // Signature depends on the curves bit length according to BSI TR-03116-2
        // 2024-10-20: even more precise Doc9309-13 chapter 2.4
        val digest = when {
            fieldSize <= 224 -> SHA224
            fieldSize <= 256 -> SHA256
            fieldSize <= 384 -> SHA384
            else -> SHA512
        }
        val ecdsaSign = this.ecPrivKey.signatureGenerator(digest = digest, ECDSA.SignatureFormat.RAW)
        return ecdsaSign.generateSignatureBlocking(data)
    }
}
