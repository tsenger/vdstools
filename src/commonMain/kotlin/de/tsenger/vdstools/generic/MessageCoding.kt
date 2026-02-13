package de.tsenger.vdstools.generic

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
enum class MessageCoding {
    C40,
    UTF8_STRING,
    BYTES,
    BYTE,
    MASKED_DATE,
    DATE,
    MRZ,
    VALIDITY_DATES,
    UNKNOWN;

    companion object {
        fun fromString(value: String): MessageCoding {
            return try {
                valueOf(value.uppercase())
            } catch (_: IllegalArgumentException) {
                throw SerializationException("Invalid value for MessageCoding: $value")
            }
        }
    }
}
