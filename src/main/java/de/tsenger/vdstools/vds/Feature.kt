package de.tsenger.vdstools.vds


class Feature(private val name: String, private val value: Any, private val coding: FeatureCoding) {
    fun coding(): FeatureCoding {
        return coding
    }

    fun name(): String {
        return name
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun valueStr(): String {
        return when (coding) {
            FeatureCoding.C40, FeatureCoding.UTF8_STRING -> value as String
            FeatureCoding.BYTE -> valueInt().toString()
            FeatureCoding.BYTES -> (value as ByteArray).toHexString()
        }
    }

    fun valueBytes(): ByteArray {
        return value as ByteArray
    }

    fun valueInt(): Int {
        return (value as Byte).toInt() and 0xFF
    }
}
