package de.tsenger.vdstools.vds


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
    val plainSignatureBytes: ByteArray
) {

    val encoded: ByteArray
        get() {
            val derSignature = DerTlv(TAG, plainSignatureBytes)
            return derSignature.encoded
        }

    companion object {
        const val TAG: Byte = 0xff.toByte()

        fun fromByteArray(rawBytes: ByteArray): VdsSignature? {
            require(rawBytes[0] == TAG) {
                "VdsSignature shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                }, but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            val derTlv = DerTlv.fromByteArray(rawBytes)
            return if (derTlv != null) VdsSignature(derTlv.value)
            else null
        }
    }
}
