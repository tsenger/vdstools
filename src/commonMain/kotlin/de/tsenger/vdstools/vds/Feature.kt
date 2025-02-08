package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder


class Feature(val name: String, private val value: Any, val coding: FeatureCoding) {

    val valueBytes: ByteArray
        get() = value as ByteArray

    val valueInt: Int
        get() = (value as Byte).toInt() and 0xFF

    @OptIn(ExperimentalStdlibApi::class)
    val valueStr: String
        get() =
            when (coding) {
                FeatureCoding.C40, FeatureCoding.UTF8_STRING -> value as String
                FeatureCoding.BYTE -> valueInt.toString()
                FeatureCoding.BYTES -> valueBytes.toHexString()
                FeatureCoding.MASKED_DATE -> DataEncoder.decodeMaskedDate(valueBytes)
                FeatureCoding.UNKNOWN -> (value as ByteArray).toHexString()
            }


}
