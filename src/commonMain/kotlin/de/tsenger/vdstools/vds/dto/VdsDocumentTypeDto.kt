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
 * @property specification The specification the message definitions were verified against, e.g.
 *   `BSI TR-03137-1 V3.0 §7.2`. Only for such document types are required messages ([MessageDto.required],
 *   [requiredOneOf]) and lengths ([MessageDto.minBytes], [MessageDto.maxBytes]) reported as format findings.
 * @property additionalFeaturesAllowed False if the specification forbids tags it does not define;
 *   such tags are then reported as not allowed instead of unknown. Only effective with [specification].
 * @property requiredOneOf Groups of tags of which at least one must be present, e.g. `[[1, 2]]` for
 *   the MRZ of an MRV-A or MRV-B visa. Only effective with [specification].
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
    var specification: String? = null,
    var additionalFeaturesAllowed: Boolean = true,
    var requiredOneOf: List<List<Int>> = emptyList(),
)
