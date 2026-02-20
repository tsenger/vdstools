package de.tsenger.vdstools.vds.dto

import kotlinx.serialization.Serializable
import de.tsenger.vdstools.generic.MessageCoding

@Serializable
data class MessageDto(
    var name: String = "",
    var tag: Int = 0,
    var coding: MessageCoding = MessageCoding.UNKNOWN,
    var required: Boolean = false,
    var minBytes: Int = 0,
    var maxBytes: Int = 0,
    val compoundTag: Int? = null,
    val compoundOrder: Int = 0,
    val messages: List<MessageDto> = emptyList(),
)
