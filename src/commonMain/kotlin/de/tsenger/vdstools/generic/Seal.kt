package de.tsenger.vdstools.generic

import de.tsenger.vdstools.idb.IcaoBarcode
import de.tsenger.vdstools.vds.DigitalSeal

abstract class Seal {
    abstract fun getMessage(name: String): Message?
    abstract fun getMessage(tag: Int): Message?
    abstract fun getPlainSignature(): ByteArray?
    abstract val rawString: String


    companion object {
        fun fromString(input: String): Seal {
            return when {
                input.startsWith(IcaoBarcode.BARCODE_IDENTIFIER) -> IcaoBarcode.Companion.fromString(input)
                input.startsWith("Ü") -> DigitalSeal.fromRawString(input)
                else -> throw IllegalArgumentException("can't parse given input to a known seal type: $input")
            }
        }
    }
}