package de.tsenger.vdstools.generic

sealed class Message {
    data class Text(val value: String) : Message()
    data class Binary(val value: ByteArray) : Message() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Binary
            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    data class Numeric(val value: Int) : Message()
}