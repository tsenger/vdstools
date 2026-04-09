package de.tsenger.vdstools

import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsSeal
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

    constructor(seal: Seal, publicKeyBytes: ByteArray, curveName: String) : this(
        seal.signatureInfo ?: throw IllegalArgumentException("Seal has no signature"),
        publicKeyBytes,
        curveName
    )

    @Deprecated("Use Verifier(seal, publicKeyBytes, curveName) instead")
    constructor(icb: IdbSeal, publicKeyBytes: ByteArray, curveName: String) : this(
        icb.payLoad.idbHeader.encoded + icb.payLoad.idbMessageGroup.encoded,
        icb.payLoad.idbSignature?.plainSignatureBytes ?: byteArrayOf(), publicKeyBytes, curveName
    )

    @Deprecated("Use Verifier(seal, publicKeyBytes, curveName) instead")
    constructor(
        vdsSeal: VdsSeal,
        publicKeyBytes: ByteArray,
        curveName: String
    ) : this(vdsSeal.signedBytes ?: byteArrayOf(), vdsSeal.signatureBytes, publicKeyBytes, curveName)


    private val tag = this::class.simpleName ?: ""

    private val keyDecoder = getCryptoProvider().get(ECDSA).publicKeyDecoder(Curve(curveName))
    private val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, publicKeyBytes)

    enum class Result {
        SignatureValid, SignatureInvalid, VerifyError,
    }


    private val fieldSize: Int
        get() = (ecPubKey.encodeToByteArrayBlocking(RAW).size - 1) * 4

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

        val digest = when (fieldSize) {
            in Int.MIN_VALUE..224 -> SHA224
            in 225..256 -> SHA256
            in 257..384 -> SHA384
            in 385..512 -> SHA512
            else -> {
                logE(tag, "Bit length of Field is out of defined value: $fieldSize")
                return Result.VerifyError
            }
        }

        val ecdsaVerify = ecPubKey.signatureVerifier(digest = digest, ECDSA.SignatureFormat.RAW)

        ecdsaVerify.tryVerifySignatureBlocking(messageBytes, signatureBytes)

        return if (ecdsaVerify.tryVerifySignatureBlocking(messageBytes, signatureBytes)) {
            Result.SignatureValid
        } else {
            Result.SignatureInvalid
        }

    }
}
