package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable

/**
 * Data class representing a VDS document type definition from VdsDocumentTypes.json.
 *
 * @property documentType The unique name for this document type
 * @property documentRef The document reference as hex string
 * @property version The version of this document type
 * @property messages List of messages defined for this document type
 * @property uuidMessageLookup If true, this document type requires UUID-based profile lookup
 * @property uuidMessageTag The tag number containing the UUID for profile lookup (default: 0)
 */
@Serializable
data class VdsDocumentTypeDto(
    var documentType: String = "",
    var documentRef: String = "",
    var version: Int = 0,
    var messages: List<MessageDto> = emptyList(),
    var uuidMessageLookup: Boolean = false,
    var uuidMessageTag: Int = 0,
    var metadataTagList: List<Int> = emptyList(),
)
