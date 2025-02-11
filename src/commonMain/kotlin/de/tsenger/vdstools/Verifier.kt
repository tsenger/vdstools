package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.IcaoBarcode
import de.tsenger.vdstools.vds.DigitalSeal
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.algorithms.EC.Curve
import dev.whyoleg.cryptography.algorithms.EC.PublicKey.Format.RAW


@OptIn(ExperimentalStdlibApi::class)
class Verifier(
    val messageBytes: ByteArray,
    val signatureBytes: ByteArray,
    publicKeyBytes: ByteArray,
    curveName: String
) {

    constructor(icb: IcaoBarcode, publicKeyBytes: ByteArray, curveName: String) : this(
        icb.payLoad.idbHeader.encoded + icb.payLoad.idbMessageGroup.encoded,
        icb.payLoad.idbSignature?.plainSignatureBytes ?: byteArrayOf(), publicKeyBytes, curveName
    )

    constructor(
        digitalSeal: DigitalSeal,
        publicKeyBytes: ByteArray,
        curveName: String
    ) : this(digitalSeal.headerAndMessageBytes, digitalSeal.signatureBytes, publicKeyBytes, curveName)


    private val log = Logger.withTag(this::class.simpleName ?: "")

    private val keyDecoder = getCryptoProvider().get(ECDSA).publicKeyDecoder(Curve(curveName))
    private val ecPubKey = keyDecoder.decodeFromByteArrayBlocking(EC.PublicKey.Format.DER, publicKeyBytes)

    enum class Result {
        SignatureValid, SignatureInvalid, VerifyError,
    }


    private val fieldSize: Int
        get() = (ecPubKey.encodeToByteArrayBlocking(RAW).size - 1) * 4

    init {

        log.v("Public Key bytes: 0x${ecPubKey.encodeToByteArrayBlocking(RAW).toHexString()}")
        log.v("Field bit length: $fieldSize")
        log.v("Message bytes: ${messageBytes.toHexString()}")
        log.v("Signature bytes: ${signatureBytes.toHexString()}")
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
                log.e("Bit length of Field is out of defined value: $fieldSize")
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
