package de.tsenger.vdstools.vds


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.ASN1Encoder
import de.tsenger.vdstools.asn1.DerTlv

class VdsSignature
/**
 * @param plainSignatureBytes signature bytes in plain format: r||s
 */(
    /**
     * Returns signature bytes in plain format: r||s
     *
     * @return r||s signature byte array
     */
    @JvmField val plainSignatureBytes: ByteArray
) {
    @OptIn(ExperimentalStdlibApi::class)
    val derSignatureBytes: ByteArray
        /**
         * Returns signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s
         * INTEGER }
         *
         * @return ASN1 DER encoded signature as byte array
         */
        get() {
            val r = plainSignatureBytes.copyOfRange(0, plainSignatureBytes.size / 2)
            val s = plainSignatureBytes.copyOfRange(plainSignatureBytes.size / 2, plainSignatureBytes.size)


            val rEncoded = ASN1Encoder.getDerInteger(r)
            val sEncoded = ASN1Encoder.getDerInteger(s)
            val derSignatureBytes = ASN1Encoder.getDerSequence(rEncoded, sEncoded)

            Logger.d("Signature sequence bytes: ${derSignatureBytes.toHexString()}")
            return derSignatureBytes
        }

    val encoded: ByteArray
        get() {
            val derSignature = DerTlv(TAG, plainSignatureBytes)
            return derSignature.encoded
        }

    companion object {
        const val TAG: Byte = 0xff.toByte()

        @JvmStatic
        fun fromByteArray(rawBytes: ByteArray): VdsSignature? {
            require(rawBytes[0] == TAG) {
                String.format(
                    "VdsSignature shall have tag %2X, but tag %2X was found instead.", TAG,
                    rawBytes[0]
                )
            }
            val derTlv = DerTlv.fromByteArray(rawBytes)
            return if (derTlv != null) VdsSignature(derTlv.value)
            else null
        }
    }
}
