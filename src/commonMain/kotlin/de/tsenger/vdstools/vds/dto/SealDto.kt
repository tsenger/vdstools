package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable

/**
 * Data class representing a seal type definition from SealCodings.json.
 *
 * @property documentType The unique name for this seal type
 * @property documentRef The document reference as hex string
 * @property version The version of this seal type
 * @property features List of features defined for this seal type
 * @property uuidFeatureLookup If true, this seal type requires UUID-based profile lookup
 * @property uuidFeatureTag The tag number containing the UUID for profile lookup (default: 0)
 */
@Serializable
data class SealDto(
    var documentType: String = "",
    var documentRef: String = "",
    var version: Int = 0,
    var features: List<FeaturesDto> = emptyList(),
    var uuidFeatureLookup: Boolean = false,
    var uuidFeatureTag: Int = 0,
)
