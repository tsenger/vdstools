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
    val messages: List<MessageDto> = emptyList(),
    /**
     * Maximum length that the specification explicitly requires, checked as a format finding:
     * characters for UTF-8 text, bytes otherwise. Null if none is given, e.g. for TR-03171 profile
     * entries without `<length>`.
     */
    val maxLength: Int? = null,
)
