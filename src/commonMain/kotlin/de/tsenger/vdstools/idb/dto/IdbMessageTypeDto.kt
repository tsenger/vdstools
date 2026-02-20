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
)
