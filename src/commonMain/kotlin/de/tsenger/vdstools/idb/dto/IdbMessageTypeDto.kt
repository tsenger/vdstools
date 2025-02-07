package de.tsenger.vdstools.idb.dto

import kotlinx.serialization.Serializable
import de.tsenger.vdstools.vds.FeatureCoding

@Serializable
data class IdbMessageTypeDto(
    var name: String = "",
    var tag: Int = 0,
    var coding: FeatureCoding = FeatureCoding.UNKNOWN,
    var minLength: Int = 0,
    var maxLength: Int = 0,
)
