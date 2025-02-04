package de.tsenger.vdstools.vds


class Feature(val name: String, private val value: Any, val coding: FeatureCoding) {

    val valueBytes: ByteArray
        get() = value as ByteArray

    //let it be public, it's used
    val valueInt: Int
        get() = (value as Byte).toInt() and 0xFF

    @OptIn(ExperimentalStdlibApi::class)
    val valueStr: String
        get() =
            when (coding) {
                FeatureCoding.C40, FeatureCoding.UTF8_STRING -> value as String
                FeatureCoding.BYTE -> valueInt.toString()
                FeatureCoding.BYTES -> valueBytes.toHexString()
                FeatureCoding.UNKNOWN -> (value as ByteArray).toHexString()
            }


}
