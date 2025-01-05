package de.tsenger.vdstools.vds

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DerTlv
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.util.encoders.Hex

import java.math.BigInteger

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
    val derSignatureBytes: ByteArray?
        /**
         * Returns signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s
         * INTEGER }
         *
         * @return ASN1 DER encoded signature as byte array
         */
        get() {
            val r = ByteArray((plainSignatureBytes.size / 2))
            val s = ByteArray((plainSignatureBytes.size / 2))

            System.arraycopy(plainSignatureBytes, 0, r, 0, r.size)
            System.arraycopy(plainSignatureBytes, r.size, s, 0, s.size)

            val v = ASN1EncodableVector()
            v.add(ASN1Integer(BigInteger(1, r)))
            v.add(ASN1Integer(BigInteger(1, s)))
            val derSeq = DERSequence(v)

            var derSignatureBytes: ByteArray? = null
            try {
                derSignatureBytes = derSeq.encoded
                Logger.d(
                    "Signature sequence bytes: 0x" + Hex.toHexString(
                        derSignatureBytes
                    )
                )
            } catch (e: Exception) {
                Logger.e("Couldn't parse r and s to DER Sequence Signature Bytes.")
            }
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
        fun fromByteArray(rawBytes: ByteArray): VdsSignature {
            require(rawBytes[0] == TAG) {
                String.format(
                    "VdsSignature shall have tag %2X, but tag %2X was found instead.", TAG,
                    rawBytes[0]
                )
            }
            val derTlv = DerTlv.fromByteArray(rawBytes)
            return VdsSignature(derTlv.value)
        }
    }
}
