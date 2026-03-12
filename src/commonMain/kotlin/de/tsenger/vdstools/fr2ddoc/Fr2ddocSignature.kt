package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.Base32

class Fr2ddocSignature private constructor(
    val signatureString: String,
    val plainSignatureBytes: ByteArray
) {
    companion object {
        fun fromString(signatureString: String): Fr2ddocSignature {
            val plainSignatureBytes = Base32.decode(signatureString)
            return Fr2ddocSignature(signatureString, plainSignatureBytes)
        }
    }
}
