package de.tsenger.vdstools_mp

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.DER
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.RAW
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.SHA224


class Signer {
    private var ecPrivKey: ECDSA.PrivateKey
    var fieldSize: Int = 0
        private set

    @OptIn(ExperimentalStdlibApi::class)
    constructor(privKeyBytes: ByteArray, curveName: String) {
        // getting platform specific provider
        val provider = getCryptoProvider()
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.privateKeyDecoder(Curve(curveName))
        ecPrivKey = keyDecoder.decodeFromByteArrayBlocking(DER, privKeyBytes)
        fieldSize = ecPrivKey.encodeToByteArrayBlocking(RAW).size * 8
    }

    fun sign(dataToSign: ByteArray): ByteArray {

        // Changed 02.12.2021:
        // Signature depends now on the curves bit length according to BSI TR-03116-2
        // 2024-10-20: even more precise Doc9309-13 chapter 2.4


        val ecdsaSign = if (fieldSize <= 224) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldSize <= 256) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldSize <= 384) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldSize <= 512) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else {
            Logger.e("Bit length of Field is out of defined value: $fieldSize")
            throw IllegalArgumentException(
                "Bit length of Field is out of defined value (224 to 512 bits): $fieldSize"
            )
        }

        Logger.i("ECDSA algorithm: $ecdsaSign")

        return ecdsaSign.generateSignatureBlocking(dataToSign)

    }
}
