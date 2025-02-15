package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv

@OptIn(ExperimentalStdlibApi::class)
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
                FeatureCoding.BYTES, FeatureCoding.UNKNOWN -> value.toHexString()
                FeatureCoding.MRZ -> {
                    val unformattedMrz = DataEncoder.decodeC40(value)
                    //val mrzLength = getFeatureDto(sealDto, tag).decodedLength
                    DataEncoder.formatMRZ(unformattedMrz, unformattedMrz.length)
                }
            }

    companion object {
        fun fromDerTlv(vdsType: String, derTlv: DerTlv): Feature? {
            return DataEncoder.encodeDerTlv(vdsType, derTlv)
        }
    }


}
