package de.tsenger.vdstools_mp

import co.touchlab.kermit.Logger
import de.tsenger.vdstools_mp.vds.DigitalSeal
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.EC.PublicKey.Format.RAW


@OptIn(ExperimentalStdlibApi::class)
class Verifier(digitalSeal: DigitalSeal, val ecPubKey: ECDSA.PublicKey) {
    enum class Result {
        SignatureValid, SignatureInvalid, VerifyError,
    }

    private val messageBytes: ByteArray = digitalSeal.headerAndMessageBytes
    private val signatureBytes: ByteArray = digitalSeal.signatureBytes

    val fieldSize: Int
        get() = (ecPubKey.encodeToByteArrayBlocking(RAW).size - 1) * 4

    init {

        Logger.d("Public Key bytes: 0x${ecPubKey.encodeToByteArrayBlocking(RAW)}")
        Logger.d("Field bit length: $fieldSize")
        Logger.d("Message bytes: ${messageBytes.toHexString()}")
        Logger.d("Signature bytes: ${signatureBytes.toHexString()}")
    }

    fun verify(): Result {
        // Changed 2024-10-20
        // Signature Algorithm is selected based on the field bit length of the curve
        // as defined in ICAO9303 p13 ch2.4

        val fieldBitLength = fieldSize
        val ecdsaVerify = if (fieldBitLength <= 224) {
            ecPubKey.signatureVerifier(digest = SHA224, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 256) {
            ecPubKey.signatureVerifier(digest = SHA256, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 384) {
            ecPubKey.signatureVerifier(digest = SHA384, ECDSA.SignatureFormat.RAW)
        } else if (fieldBitLength <= 512) {
            ecPubKey.signatureVerifier(digest = SHA512, ECDSA.SignatureFormat.RAW)
        } else {
            Logger.e("Bit length of Field is out of defined value: $fieldBitLength")
            return Result.VerifyError
        }

        ecdsaVerify.tryVerifySignatureBlocking(messageBytes, signatureBytes)

        return if (ecdsaVerify.tryVerifySignatureBlocking(messageBytes, signatureBytes)) {
            Result.SignatureValid
        } else {
            Result.SignatureInvalid
        }

    }
}
