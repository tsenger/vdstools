package de.tsenger.vdstools.generic

import de.tsenger.vdstools.idb.IcaoBarcode
import de.tsenger.vdstools.vds.DigitalSeal

abstract class Seal {
    abstract fun getMessage(name: String): Message?
    abstract fun getMessage(tag: Int): Message?

    abstract val documentType: String
    abstract val messageList: List<Message>
    abstract val issuingCountry: String
    abstract val signatureInfo: SignatureInfo?
    abstract val signedBytes: ByteArray?
    abstract val rawString: String


    companion object {
        fun fromString(input: String): Seal {
            return when {
                input.startsWith(IcaoBarcode.BARCODE_IDENTIFIER) -> IcaoBarcode.fromString(input)
                input.startsWith("Ü") -> DigitalSeal.fromRawString(input)
                else -> throw IllegalArgumentException("can't parse given input to a known seal type: $input")
            }
        }
    }
}