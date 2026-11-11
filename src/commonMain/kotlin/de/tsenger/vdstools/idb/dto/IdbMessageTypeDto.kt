package de.tsenger.vdstools.idb.dto

import kotlinx.serialization.Serializable
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.MessageDto

@Serializable
data class IdbMessageTypeDto(
    var name: String = "",
    var tag: Int = 0,
    var coding: MessageCoding = MessageCoding.UNKNOWN,
    var minBytes: Int = 0,
    var maxBytes: Int = 0,
    val messages: List<MessageDto> = emptyList(),
    /** Country of a national message type (e.g. `D` for BSI TR-03137 0x80–0x86), null for an ICAO type. */
    var country: String? = null,
)
