package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable

@Serializable
data class SealDto(
    var documentType: String = "",
    var documentRef: String = "",
    var version: Int = 0,
    var features: List<FeaturesDto> = emptyList(),
)
