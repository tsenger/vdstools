package de.tsenger.vdstools.idb

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.ASN1Encoder
import de.tsenger.vdstools.asn1.DerTlv

class IdbSignature(
    val plainSignatureBytes: ByteArray
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
        const val TAG: Byte = 0x7F

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray): IdbSignature? {
            require(rawBytes[0] == TAG) {
                "IdbSignature shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                } but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            val derTlv = DerTlv.fromByteArray(rawBytes)
            return if (derTlv != null) IdbSignature(derTlv.value)
            else null
        }
    }
}
