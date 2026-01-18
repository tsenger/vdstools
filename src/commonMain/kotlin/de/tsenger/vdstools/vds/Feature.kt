package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder

class Feature(val tag: Int, val name: String, private val value: ByteArray, val coding: FeatureCoding) {

    val valueBytes: ByteArray
        get() = value

    val valueInt: Int
        get() = value[0].toInt() and 0xFF

    val valueStr: String
        get() =
            when (coding) {
                FeatureCoding.BYTE -> valueInt.toString()
                FeatureCoding.C40 -> DataEncoder.decodeC40(value)
                FeatureCoding.UTF8_STRING -> value.decodeToString()
                FeatureCoding.MASKED_DATE -> DataEncoder.decodeMaskedDate(value)
                FeatureCoding.DATE -> DataEncoder.decodeDate(value).toString()
                FeatureCoding.BYTES, FeatureCoding.UNKNOWN -> value.toHexString()
                FeatureCoding.MRZ -> {
                    val unformattedMrz = DataEncoder.decodeC40(value)
                    //val mrzLength = getFeatureDto(sealDto, tag).decodedLength
                    DataEncoder.formatMRZ(unformattedMrz, unformattedMrz.length)
                }
            }
}
