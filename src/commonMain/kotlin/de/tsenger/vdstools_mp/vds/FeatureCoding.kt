package de.tsenger.vdstools_mp.vds

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
enum class FeatureCoding {
    C40,
    UTF8_STRING,
    BYTES,
    BYTE,
    UNKNOWN;

    companion object {
        fun fromString(value: String): FeatureCoding {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                throw SerializationException("Invalid value for FeatureCoding: $value")
            }
        }
    }
}
