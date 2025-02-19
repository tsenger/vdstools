package de.tsenger.vdstools.vds

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
enum class FeatureCoding {
    C40,
    UTF8_STRING,
    BYTES,
    BYTE,
    MASKED_DATE,
    DATE,
    MRZ,
    UNKNOWN;

    companion object {
        fun fromString(value: String): FeatureCoding {
            return try {
                valueOf(value.uppercase())
            } catch (_: IllegalArgumentException) {
                throw SerializationException("Invalid value for FeatureCoding: $value")
            }
        }
    }
}
