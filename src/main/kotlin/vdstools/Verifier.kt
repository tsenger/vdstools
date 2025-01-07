package vdstools

import co.touchlab.kermit.Logger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import vdstools.vds.DigitalSeal
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey

@OptIn(ExperimentalStdlibApi::class)
class Verifier(digitalSeal: DigitalSeal, sealSignerCertificate: X509Certificate) {
    enum class Result {
        SignatureValid, SignatureInvalid, VerifyError,
    }

    private val ecPubKey: ECPublicKey
    private val fieldBitLength: Int
    private val messageBytes: ByteArray
    private val signatureBytes: ByteArray

    var signatureAlgorithmName: String = "SHA256WITHECDSA"


    init {
        Security.addProvider(BouncyCastleProvider())
        require(sealSignerCertificate.publicKey is ECPublicKey) { "Certificate should contain EC public key!" }
        ecPubKey = sealSignerCertificate.publicKey as ECPublicKey
        this.fieldBitLength = ecPubKey.params.curve.field.fieldSize
        this.messageBytes = digitalSeal.headerAndMessageBytes
        this.signatureBytes = digitalSeal.signatureBytes

        Logger.d("Public Key bytes: 0x${ecPubKey.encoded.toHexString()}")
        Logger.d("Field bit length: ${this.fieldBitLength}")
        Logger.d("Message bytes: ${messageBytes.toHexString()}")
        Logger.d("Signature bytes: ${signatureBytes.toHexString()}")
    }

    fun verify(): Result {
        // Changed 2024-10-20
        // Signature Algorithm is selected based on the field bit length of the curve
        // as defined in ICAO9303 p13 ch2.4

        signatureAlgorithmName = if (fieldBitLength <= 224) {
            "SHA224withPLAIN-ECDSA"
        } else if (fieldBitLength <= 256) {
            "SHA256withPLAIN-ECDSA"
        } else if (fieldBitLength <= 384) {
            "SHA384withPLAIN-ECDSA"
        } else if (fieldBitLength <= 512) {
            "SHA512withPLAIN-ECDSA"
        } else {
            Logger.e("Bit length of Field is out of defined value: $fieldBitLength")
            return Result.VerifyError
        }

        try {
            Logger.d("Verify with signatureAlgorithmName: $signatureAlgorithmName")
            val ecdsaVerify = Signature.getInstance(signatureAlgorithmName, "BC")
            ecdsaVerify.initVerify(ecPubKey)
            ecdsaVerify.update(messageBytes)

            return if (ecdsaVerify.verify(signatureBytes)) {
                Result.SignatureValid
            } else {
                Result.SignatureInvalid
            }
        } catch (e1: Exception) {
            Logger.e("Verify error: ${e1.localizedMessage}")
            return Result.VerifyError
        }
    }
}
