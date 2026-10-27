package de.tsenger.vdstools

import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.internal.ecFieldBitSize
import de.tsenger.vdstools.internal.logE
import de.tsenger.vdstools.internal.logV
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PublicKey.Format.RAW


/**
 * Verifies an ECDSA signature over a message.
 *
 * @param messageBytes the raw bytes that were signed
 * @param signatureBytes the ECDSA signature in plain r||s format (not ASN.1/DER-encoded)
 * @param publicKeyBytes the signer's public key as DER-encoded SubjectPublicKeyInfo
 * @param curveName the curve name, e.g. "brainpoolP224r1"
 */
@OptIn(ExperimentalStdlibApi::class)
class Verifier(
    val messageBytes: ByteArray,
    val signatureBytes: ByteArray,
    publicKeyBytes: ByteArray,
    curveName: String
) {

    constructor(signatureInfo: SignatureInfo, publicKeyBytes: ByteArray, curveName: String) : this(
        signatureInfo.signedBytes,
        signatureInfo.plainSignatureBytes,
        publicKeyBytes,
        curveName
    )

    /** @throws IllegalArgumentException if the seal has no signature (e.g. an unsigned IDB seal) */
    constructor(seal: Seal, publicKeyBytes: ByteArray, curveName: String) : this(
        seal.signatureInfo ?: throw IllegalArgumentException("Seal has no signature"),
        publicKeyBytes,
        curveName
    )


    private val tag = this::class.simpleName ?: ""

    private val keyDecoder = getCryptoProvider().get(ECDSA).publicKeyDecoder(Curve(curveName))
    private val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, publicKeyBytes)

    enum class Result {
        SignatureValid, SignatureInvalid, VerifyError,
    }


    private val fieldSize: Int?
        get() = ecFieldBitSize((ecPubKey.encodeToByteArrayBlocking(RAW).size - 1) / 2)

    init {

        logV(tag, "Public Key bytes: 0x${ecPubKey.encodeToByteArrayBlocking(RAW).toHexString()}")
        logV(tag, "Field bit length: $fieldSize")
        logV(tag, "Message bytes: ${messageBytes.toHexString()}")
        logV(tag, "Signature bytes: ${signatureBytes.toHexString()}")
    }

    fun verify(): Result {
        // Changed 2024-10-20
        // Signature Algorithm is selected based on the field bit length of the curve
        // as defined in ICAO9303 p13 ch2.4

        val fieldSize = fieldSize
        val digest = when {
            fieldSize == null -> {
                logE(tag, "No supported curve for public key length")
                return Result.VerifyError
            }
            fieldSize <= 224 -> SHA224
            fieldSize <= 256 -> SHA256
            fieldSize <= 384 -> SHA384
            else -> SHA512
        }

        val ecdsaVerify = ecPubKey.signatureVerifier(digest = digest, ECDSA.SignatureFormat.RAW)

        return if (ecdsaVerify.tryVerifySignatureBlocking(messageBytes, signatureBytes)) {
            Result.SignatureValid
        } else {
            Result.SignatureInvalid
        }

    }
}
