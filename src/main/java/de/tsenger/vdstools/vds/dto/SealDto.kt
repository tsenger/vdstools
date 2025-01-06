package de.tsenger.vdstools.vds.dto

data class SealDto(
    var documentType: String? = null,
    var documentRef: String? = null,
    var version: Int = 0,
    var features: List<FeaturesDto>? = null,
)
