package de.tsenger.vdstools.generic

import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.FeatureValue

class Message(
    val messageTypeTag: Int,
    val messageTypeName: String,
    val coding: FeatureCoding,
    val value: FeatureValue
) {
    override fun toString(): String = "$messageTypeName: $value"
}
