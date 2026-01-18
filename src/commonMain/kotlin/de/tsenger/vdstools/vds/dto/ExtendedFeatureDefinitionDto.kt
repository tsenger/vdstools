package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable

/**
 * Data class representing an extended feature definition for UUID-based seal lookup.
 *
 * Extended feature definitions allow for a two-stage lookup process as specified in TR-03171:
 * 1. The documentRef in the header determines the base type (e.g., ADMINISTRATIVE_DOCUMENTS)
 * 2. The UUID in Tag 0 (Dokumentenprofilnummer) determines the specific definition (e.g., MELDEBESCHEINIGUNG)
 *
 * This extends the limited 256-value space of Document Feature Definition Reference in the header
 * by using UUIDs in the message zone for dynamic profile registration.
 *
 * @property definitionId UUID as hex string without dashes (32 characters)
 * @property definitionName The effective vdsType name (e.g., "MELDEBESCHEINIGUNG")
 * @property baseDocumentType Link to the base type in SealCodings.json
 * @property version Definition version
 * @property features Definition-specific features (base type features are inherited)
 */
@Serializable
data class ExtendedFeatureDefinitionDto(
    val definitionId: String = "",
    val definitionName: String = "",
    val baseDocumentType: String = "",
    val version: Int = 1,
    val features: List<FeaturesDto> = emptyList()
)
