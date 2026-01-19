package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.FeatureValue

class IdbFeature(
    val tag: Int,
    val name: String,
    val coding: FeatureCoding,
    val value: FeatureValue
) {
    val encoded: ByteArray
        get() = DerTlv(tag.toByte(), value.rawBytes).encoded

    override fun toString(): String = "$name: $value"
}
