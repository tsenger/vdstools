package de.tsenger.vdstools.idb.dto

import kotlinx.serialization.Serializable
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.MessageDto

@Serializable
data class IdbMessageTypeDto(
    var name: String = "",
    var tag: Int = 0,
    var coding: MessageCoding = MessageCoding.UNKNOWN,
    /**
     * The specification the message type was verified against, e.g. `BSI TR-03137-1 V3.0 §11.2.2`.
     * Only for such message types are [minBytes] and [maxBytes] reported as format findings.
     */
    var specification: String? = null,
    /** Minimum length of the value in bytes; only checked with [specification]. */
    var minBytes: Int = 0,
    /** Maximum length of the value in bytes, 0 if the specification gives none; only checked with [specification]. */
    var maxBytes: Int = 0,
    val messages: List<MessageDto> = emptyList(),
    /**
     * True for the message type whose value is the national document type (e.g. BSI TR-03137 0x86
     * `NATIONAL_DOCUMENT_IDENTIFIER`); it is looked up in the IDB document types.
     */
    var identifiesDocumentType: Boolean = false,
)
