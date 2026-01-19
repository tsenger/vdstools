package de.tsenger.vdstools.vds

class Feature(
    val tag: Int,
    val name: String,
    val coding: FeatureCoding,
    val value: FeatureValue
) {
    override fun toString(): String = "$name: $value"
}
