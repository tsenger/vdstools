package de.tsenger.vdstools.vds.dto

import de.tsenger.vdstools.vds.FeatureCoding

data class FeaturesDto(
    var name: String? = null,
    var tag: Int = 0,
    var coding: FeatureCoding? = null,
    var decodedLength: Int = 0,
    var required: Boolean = false,
    var minLength: Int = 0,
    var maxLength: Int = 0,
)
