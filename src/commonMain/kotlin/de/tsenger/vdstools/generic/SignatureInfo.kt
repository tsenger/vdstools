package de.tsenger.vdstools.generic

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.ASN1Encoder
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalStdlibApi::class)
data class SignatureInfo(
    val plainSignatureBytes: ByteArray,
    val signerCertificateReference: String,
    val signingDate: LocalDate,
    var signerCertificateBytes: ByteArray? = null,
    var signatureAlgorithm: String? = null
) {
    private val log = Logger.withTag(this::class.simpleName ?: "")


    val derSignatureBytes: ByteArray
        get() {
            val r = plainSignatureBytes.copyOfRange(0, plainSignatureBytes.size / 2)
            val s = plainSignatureBytes.copyOfRange(plainSignatureBytes.size / 2, plainSignatureBytes.size)

            val rEncoded = ASN1Encoder.getDerInteger(r)
            val sEncoded = ASN1Encoder.getDerInteger(s)
            val derSignatureBytes = ASN1Encoder.getDerSequence(rEncoded, sEncoded)

            log.d("DER Signature bytes: ${derSignatureBytes.toHexString()}")
            return derSignatureBytes
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SignatureInfo

        if (!plainSignatureBytes.contentEquals(other.plainSignatureBytes)) return false
        if (signerCertificateReference != other.signerCertificateReference) return false
        if (signingDate != other.signingDate) return false
        if (!signerCertificateBytes.contentEquals(other.signerCertificateBytes)) return false
        if (signatureAlgorithm != other.signatureAlgorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = plainSignatureBytes.contentHashCode()
        result = 31 * result + signerCertificateReference.hashCode()
        result = 31 * result + signingDate.hashCode()
        result = 31 * result + (signerCertificateBytes?.contentHashCode() ?: 0)
        result = 31 * result + (signatureAlgorithm?.hashCode() ?: 0)
        return result
    }
}

