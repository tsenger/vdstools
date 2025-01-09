package de.tsenger.vdstools_mp.vds.dto

import kotlinx.serialization.Serializable
import de.tsenger.vdstools_mp.vds.FeatureCoding

@Serializable
data class FeaturesDto(
    var name: String = "",
    var tag: Int = 0,
    var coding: FeatureCoding = FeatureCoding.UNKNOWN,
    var decodedLength: Int = 0,
    var required: Boolean = false,
    var minLength: Int = 0,
    var maxLength: Int = 0,
)
