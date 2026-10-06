package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable

/**
 * Data class representing an extended message definition for UUID-based seal lookup.
 *
 * Extended message definitions allow for a two-stage lookup process as specified in TR-03171:
 * 1. The documentRef in the header determines the base type (e.g., ADMINISTRATIVE_DOCUMENTS_V8)
 * 2. The UUID in Tag 0 (Dokumentenprofilnummer) determines the specific definition (e.g., MELDEBESCHEINIGUNG)
 *
 * This extends the limited 256-value space of Document Feature Definition Reference in the header
 * by using UUIDs in the message zone for dynamic profile registration.
 *
 * @property definitionId UUID as hex string without dashes (32 characters)
 * @property definitionName The effective vdsType name (e.g., "MELDEBESCHEINIGUNG")
 * @property baseDocumentType Link to the base type in VdsDocumentTypes.json
 * @property version Definition version
 * @property messages Definition-specific messages (base type messages are inherited)
 * @property validFromPresent TR-03171 v0.9: `true` if VALID_FROM (tag 0x01) must be present, `false` if it
 *   must not be present, `null` if the profile makes no statement (e.g. legacy 0xC8 profiles)
 * @property validToPresent Same as [validFromPresent] for VALID_TO (tag 0x02)
 */
@Serializable
data class VdsProfileDefinitionDto(
    val definitionId: String = "",
    val definitionName: String = "",
    val baseDocumentType: String = "",
    val version: Int = 1,
    val messages: List<MessageDto> = emptyList(),
    val validFromPresent: Boolean? = null,
    val validToPresent: Boolean? = null
)
