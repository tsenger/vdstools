package de.tsenger.vdstools.idb.dto

import kotlinx.serialization.Serializable

/**
 * Reference to an IDB message type within a national document type definition.
 *
 * This is not a full message definition but a reference by name to an entry in
 * `IdbMessageTypes.json`. It declares which message types are expected in the
 * message group for a particular national document type, and whether each is mandatory.
 *
 * @property name The message type name as defined in `IdbMessageTypes.json` (e.g., `"FACE_IMAGE"`)
 * @property required Whether this message type must be present in the message group
 */
@Serializable
data class IdbMessageTypeRef(
    val name: String = "",
    val required: Boolean = false,
)

/**
 * Data class representing a document type definition from `IdbDocumentTypes.json`.
 *
 * In IDB (ICAO TR-IDB / BSI TR-03137), a message group can contain multiple message types
 * belonging to a single document. The national document type (the value of the message type marked
 * `identifiesDocumentType`, BSI TR-03137: `NATIONAL_DOCUMENT_IDENTIFIER`, tag 0x86) declares which kind of document the message group represents
 * and which message types are expected within it.
 *
 * @property name The document type name (e.g., `"SUBSTITUTE_IDENTITY_DOCUMENT"`)
 * @property tag The numeric value of the `NATIONAL_DOCUMENT_IDENTIFIER` (tag 0x86)
 * @property messages The IDB message types expected in the message group for this document type.
 *   Empty if the expected message combination is not defined.
 * @property country Country whose national document types this belongs to (e.g. `D`), null if not national.
 * @property specification The specification the expected messages were verified against, e.g.
 *   `BSI TR-03137-1 V3.0 §15.2.2`. Only for such document types are missing required messages reported
 *   as format findings.
 * @property additionalMessagesAllowed False if the specification forbids message types it does not list
 *   for this document type; they are then reported as not allowed. Only effective with [specification].
 */
@Serializable
data class IdbDocumentTypeDto(
    val name: String,
    val tag: Int,
    val messages: List<IdbMessageTypeRef> = emptyList(),
    val country: String? = null,
    val specification: String? = null,
    val additionalMessagesAllowed: Boolean = true,
)