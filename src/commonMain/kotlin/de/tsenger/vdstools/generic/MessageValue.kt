package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

sealed class MessageValue {
    abstract val rawBytes: ByteArray
    abstract val decoded: Any

    override fun toString(): String = decoded.toString()

    data class ByteValue(val value: Int, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: Int get() = value

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByteValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value + rawBytes.contentHashCode()
    }

    data class StringValue(val value: String, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: String get() = value

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
    }

    data class DateValue(val date: LocalDate, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: LocalDate get() = date

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DateValue) return false
            return date == other.date && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * date.hashCode() + rawBytes.contentHashCode()
    }

    data class MaskedDateValue(val value: String, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: String get() = value

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MaskedDateValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
    }

    data class MrzValue(val mrz: String, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: String get() = mrz

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MrzValue) return false
            return mrz == other.mrz && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * mrz.hashCode() + rawBytes.contentHashCode()
    }

    data class ValidityDatesValue(
        val validFrom: LocalDate?,
        val validTo: LocalDate?,
        override val rawBytes: ByteArray
    ) : MessageValue() {
        override val decoded: String get() = toString()

        override fun toString(): String {
            return when {
                validFrom != null && validTo != null -> "$validFrom - $validTo"
                validFrom != null -> validFrom.toString()
                validTo != null -> validTo.toString()
                else -> ""
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ValidityDatesValue) return false
            return validFrom == other.validFrom && validTo == other.validTo && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int =
            31 * (31 * (validFrom?.hashCode() ?: 0) + (validTo?.hashCode() ?: 0)) + rawBytes.contentHashCode()

        companion object {
            fun of(validFrom: LocalDate?, validTo: LocalDate?): ValidityDatesValue {
                val raw = buildString {
                    if (validFrom != null) append(formatDate(validFrom))
                    append('\u0000')
                    if (validTo != null) append(formatDate(validTo))
                }.encodeToByteArray()
                return ValidityDatesValue(validFrom, validTo, raw)
            }

            fun parse(bytes: ByteArray): ValidityDatesValue {
                val str = bytes.decodeToString()
                val parts = str.split('\u0000', limit = 2)
                val fromStr = parts.getOrNull(0)?.takeIf { it.isNotEmpty() }
                val toStr = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                val validFrom = fromStr?.let { parseDate(it) }
                val validTo = toStr?.let { parseDate(it) }
                return ValidityDatesValue(validFrom, validTo, bytes)
            }

            private fun parseDate(s: String): LocalDate {
                require(s.length == 8) { "Invalid date string length: ${s.length}" }
                val year = s.substring(0, 4).toInt()
                val month = s.substring(4, 6).toInt()
                val day = s.substring(6, 8).toInt()
                return LocalDate(year, month, day)
            }

            private fun formatDate(date: LocalDate): String {
                val y = date.year.toString().padStart(4, '0')
                val m = date.month.number.toString().padStart(2, '0')
                val d = date.day.toString().padStart(2, '0')
                return "$y$m$d"
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    data class BytesValue(override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: String get() = rawBytes.toHexString()

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BytesValue) return false
            return rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = rawBytes.contentHashCode()
    }

    companion object {
        /**
         * Factory method: Creates MessageValue based on coding type.
         * @param bytes The raw byte array to decode
         * @param coding The encoding type of the bytes
         * @param mrzLength Optional MRZ length for correct formatting (88 for MRVA, 72 for MRVB)
         */
        fun fromBytes(bytes: ByteArray, coding: MessageCoding, mrzLength: Int? = null): MessageValue {
            return try {
                when (coding) {
                    MessageCoding.BYTE -> {
                        if (bytes.isEmpty()) BytesValue(bytes)
                        else ByteValue(bytes[0].toInt() and 0xFF, bytes)
                    }

                    MessageCoding.C40 -> StringValue(DataEncoder.decodeC40(bytes), bytes)
                    MessageCoding.UTF8_STRING -> StringValue(bytes.decodeToString(), bytes)
                    MessageCoding.DATE -> DateValue(DataEncoder.decodeDate(bytes), bytes)
                    MessageCoding.MASKED_DATE -> MaskedDateValue(
                        DataEncoder.decodeMaskedDate(bytes),
                        bytes
                    )

                    MessageCoding.MRZ -> {
                        val unformattedMrz = DataEncoder.decodeC40(bytes)
                        val length = mrzLength ?: unformattedMrz.length
                        MrzValue(DataEncoder.formatMRZ(unformattedMrz, length), bytes)
                    }

                    MessageCoding.VALIDITY_DATES -> ValidityDatesValue.parse(bytes)
                    MessageCoding.BYTES, MessageCoding.UNKNOWN -> BytesValue(bytes)
                }
            } catch (e: Exception) {
                // If decoding fails, fall back to raw bytes
                BytesValue(bytes)
            }
        }
    }
}
