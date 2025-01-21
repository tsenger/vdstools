package de.tsenger.vdstools_mp.idb

import de.tsenger.vdstools_mp.asn1.DerTlv

class IdbSignerCertificate(val certBytes: ByteArray) {

    val encoded: ByteArray
        get() = DerTlv(TAG, certBytes).encoded

    companion object {
        const val TAG: Byte = 0x7E

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray): IdbSignerCertificate {
            val derTlv = DerTlv.fromByteArray(rawBytes)
            require(derTlv != null) {
                "Couldn't parse given bytes to IdbSignerCertificate: DER TLV parsing failed!"
            }
            require(derTlv.tag == TAG) {
                "IdbSignerCertificate shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                }, but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            return IdbSignerCertificate(derTlv.value)
        }
    }
}
