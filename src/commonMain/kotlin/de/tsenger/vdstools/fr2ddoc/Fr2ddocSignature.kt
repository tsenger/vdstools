package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.Base32

class Fr2ddocSignature private constructor(
    val rawString: String,
    val bytes: ByteArray
) {
    companion object {
        fun parse(signatureString: String): Fr2ddocSignature {
            val bytes = Base32.decode(signatureString)
            return Fr2ddocSignature(signatureString, bytes)
        }
    }
}
