package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv

internal class IdbSignature(
    val plainSignatureBytes: ByteArray
) {
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
