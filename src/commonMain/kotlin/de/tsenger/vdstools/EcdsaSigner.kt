package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.DER
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.RAW


class EcdsaSigner(privKeyBytes: ByteArray, curveName: String) : Signer {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var ecPrivKey: ECDSA.PrivateKey
    override var fieldSize: Int = 0
        private set

    init {
        // getting platform specific provider
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.privateKeyDecoder(Curve(curveName))
        ecPrivKey = keyDecoder.decodeFromByteArrayBlocking(DER, privKeyBytes)
        fieldSize = ecPrivKey.encodeToByteArrayBlocking(RAW).size * 8
        if (fieldSize !in 224..512) {
            log.e("Bit length of Field is out of defined value: $fieldSize")
            throw IllegalArgumentException(
                "Bit length of Field is out of defined value (224 to 512 bits): $fieldSize"
            )
        }
    }

    override fun sign(data: ByteArray): ByteArray {
        // Signature depends on the curves bit length according to BSI TR-03116-2
        // 2024-10-20: even more precise Doc9309-13 chapter 2.4
        val digest = when (fieldSize) {
            in Int.MIN_VALUE..224 -> SHA224
            in 225..256 -> SHA256
            in 257..384 -> SHA384
            in 385..512 -> SHA512
            else -> throw IllegalArgumentException("Ungültige fieldSize: $fieldSize")
        }
        val ecdsaSign = this.ecPrivKey.signatureGenerator(digest = digest, ECDSA.SignatureFormat.RAW)
        return ecdsaSign.generateSignatureBlocking(data)
    }
}
