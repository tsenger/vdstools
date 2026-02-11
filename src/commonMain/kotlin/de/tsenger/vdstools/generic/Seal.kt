package de.tsenger.vdstools.generic

import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsSeal

abstract class Seal {
    abstract fun getMessage(name: String): Message?
    abstract fun getMessage(tag: Int): Message?

    abstract val documentType: String

    open val baseDocumentType: String?
        get() = null
    abstract val messageList: List<Message>
    abstract val issuingCountry: String
    abstract val signatureInfo: SignatureInfo?
    abstract val signedBytes: ByteArray?
    abstract val rawString: String


    companion object {
        fun fromString(input: String): Seal {
            return when {
                input.startsWith(IdbSeal.BARCODE_IDENTIFIER) -> IdbSeal.fromString(input)
                input.startsWith(IdbSeal.BARCODE_IDENTIFIER_OLD) -> IdbSeal.fromString(input)
                input.startsWith("Ü") -> VdsSeal.fromRawString(input)
                else -> throw IllegalArgumentException("can't parse given input to a known seal type: $input")
            }
        }
    }
}