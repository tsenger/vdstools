package de.tsenger.vdstools_mp

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PrivateKey.Format.RAW
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.SHA224


class Signer {
    private var ecPrivKey: ECDSA.PrivateKey

    constructor(privKeyBytes: ByteArray, curveName: String) {
        // getting default provider
        val provider = CryptographyProvider.Default
        val ecdsa = provider.get(ECDSA)
        val keyDecoder = ecdsa.privateKeyDecoder(Curve(curveName))
        ecPrivKey = keyDecoder.decodeFromByteArrayBlocking(RAW, privKeyBytes)
    }


    val fieldSize: Int
        get() = (ecPrivKey.encodeToByteArrayBlocking(RAW).size - 1) * 4


    fun sign(dataToSign: ByteArray): ByteArray {


        // Changed 02.12.2021:
        // Signature depends now on the curves bit length according to BSI TR-03116-2
        // 2024-10-20: even more precise Doc9309-13 chapter 2.4
        val fieldBitLength = fieldSize
        val ecdsaSign = if (fieldBitLength <= 224) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 256) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 384) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 512) {
            this.ecPrivKey.signatureGenerator(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else {
            Logger.e("Bit length of Field is out of defined value: $fieldBitLength")
            throw IllegalArgumentException(
                "Bit length of Field is out of defined value (224 to 512 bits): $fieldBitLength"
            )
        }

        Logger.i("ECDSA algorithm: $ecdsaSign")

        return ecdsaSign.generateSignatureBlocking(dataToSign)

    }
}
