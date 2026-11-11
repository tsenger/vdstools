package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

    data class IntegerValue(val value: Long, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: Long get() = value

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IntegerValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
    }

    data class BooleanValue(val value: Boolean, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: Boolean get() = value

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BooleanValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
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

    data class DateTimeValue(val dateTime: LocalDateTime, override val rawBytes: ByteArray) : MessageValue() {
        override val decoded: LocalDateTime get() = dateTime

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DateTimeValue) return false
            return dateTime == other.dateTime && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * dateTime.hashCode() + rawBytes.contentHashCode()
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

    /**
     * Duration of stay of an ICAO visa (ICAO Doc 9303-13, visa message tag 0x04), encoded as exactly
     * three unsigned bytes `days, months, years`.
     *
     * The triples `00 00 00`, `FF FF FF` and `FE FE FE` are reserved and map to the special [Kind]s
     * [Kind.UNTIL_VALID_TO], [Kind.DETERMINED_AT_ENTRY] and [Kind.TRANSIT_WITHOUT_ENTRY]. Every other
     * triple is a [Kind.DURATION] whose length is the sum of [days], [months] and [years], counted from
     * the date of entry.
     *
     * [days], [months] and [years] are only meaningful for [Kind.DURATION]; for the special kinds they
     * are 0 (the raw bytes stay available via [rawBytes]).
     *
     * [toString] is a neutral, non-localized representation: e.g. `30d`, `1y 6m 15d`, or the
     * [Kind] name for the special kinds. Localized display is up to the application.
     */
    data class DurationOfStayValue(
        val kind: Kind,
        val days: Int,
        val months: Int,
        val years: Int,
        override val rawBytes: ByteArray
    ) : MessageValue() {

        enum class Kind {
            /** `00 00 00`: the MRZ "valid until" date is the last day the holder may stay in the country. */
            UNTIL_VALID_TO,

            /**
             * `FF FF FF`: "valid until" is the last day entry may be requested at the border; the
             * duration of stay is determined by the authorities at entry.
             */
            DETERMINED_AT_ENTRY,

            /** `FE FE FE`: transit visa without permission to enter the transit country. */
            TRANSIT_WITHOUT_ENTRY,

            /** Regular duration: [days] + [months] + [years] from the date of entry. */
            DURATION
        }

        override val decoded: String get() = toString()

        override fun toString(): String {
            if (kind != Kind.DURATION) return kind.name
            return buildList {
                if (years != 0) add("${years}y")
                if (months != 0) add("${months}m")
                if (days != 0) add("${days}d")
            }.joinToString(" ")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DurationOfStayValue) return false
            return kind == other.kind && days == other.days && months == other.months &&
                    years == other.years && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int {
            var result = kind.hashCode()
            result = 31 * result + days
            result = 31 * result + months
            result = 31 * result + years
            result = 31 * result + rawBytes.contentHashCode()
            return result
        }

        companion object {
            /**
             * Creates a regular [Kind.DURATION] value. Each component must be in 0..255, and the
             * resulting triple must not be one of the reserved triples.
             */
            fun of(days: Int, months: Int, years: Int): DurationOfStayValue {
                require(days in 0..255 && months in 0..255 && years in 0..255) {
                    "days, months and years must each be in 0..255"
                }
                val value = parse(byteArrayOf(days.toByte(), months.toByte(), years.toByte()))
                require(value.kind == Kind.DURATION) {
                    "($days, $months, $years) is a reserved triple; use of(Kind) instead"
                }
                return value
            }

            /** Creates a value for one of the special kinds (not [Kind.DURATION]). */
            fun of(kind: Kind): DurationOfStayValue {
                val b = when (kind) {
                    Kind.UNTIL_VALID_TO -> 0x00
                    Kind.DETERMINED_AT_ENTRY -> 0xFF
                    Kind.TRANSIT_WITHOUT_ENTRY -> 0xFE
                    Kind.DURATION -> throw IllegalArgumentException("Use of(days, months, years) for Kind.DURATION")
                }.toByte()
                return DurationOfStayValue(kind, 0, 0, 0, byteArrayOf(b, b, b))
            }

            /**
             * Parses the three-byte encoding.
             *
             * @throws IllegalArgumentException if [bytes] is not exactly three bytes long.
             */
            fun parse(bytes: ByteArray): DurationOfStayValue {
                require(bytes.size == 3) { "expected three bytes for duration of stay, got ${bytes.size}" }
                val d = bytes[0].toInt() and 0xFF
                val m = bytes[1].toInt() and 0xFF
                val y = bytes[2].toInt() and 0xFF
                val kind = when {
                    d == 0x00 && m == 0x00 && y == 0x00 -> Kind.UNTIL_VALID_TO
                    d == 0xFF && m == 0xFF && y == 0xFF -> Kind.DETERMINED_AT_ENTRY
                    d == 0xFE && m == 0xFE && y == 0xFE -> Kind.TRANSIT_WITHOUT_ENTRY
                    else -> Kind.DURATION
                }
                return if (kind == Kind.DURATION) DurationOfStayValue(kind, d, m, y, bytes)
                else DurationOfStayValue(kind, 0, 0, 0, bytes)
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
         * Factory method: Creates a [MessageValue] from raw bytes based on the given coding.
         *
         * @param bytes The raw encoded bytes to decode.
         * @param coding The encoding type; drives both decode algorithm and display formatting.
         * @param mrzLength Optional line-split length for [MessageCoding.MRZ]. When null, the
         *   decoded string length is used (i.e. split at half). Unused for [MessageCoding.MRZ_MRVA]
         *   and [MessageCoding.MRZ_MRVB], which have fixed line lengths defined by their coding.
         */
        fun fromBytes(bytes: ByteArray, coding: MessageCoding, mrzLength: Int? = null): MessageValue {
            return try {
                when (coding) {
                    MessageCoding.BYTE -> {
                        if (bytes.isEmpty()) BytesValue(bytes)
                        else ByteValue(bytes[0].toInt() and 0xFF, bytes)
                    }

                    MessageCoding.INTEGER -> IntegerValue(DataEncoder.decodeInteger(bytes), bytes)
                    MessageCoding.BOOLEAN -> BooleanValue(DataEncoder.decodeBoolean(bytes), bytes)
                    MessageCoding.C40 -> StringValue(DataEncoder.decodeC40(bytes), bytes)
                    MessageCoding.UTF8_STRING -> StringValue(bytes.decodeToString(throwOnInvalidSequence = true), bytes)
                    MessageCoding.DATE -> DateValue(DataEncoder.decodeDate(bytes), bytes)
                    // TR-03171 v0.9: 8-byte YYYYMMDD UTF-8 string; decoded result is the same DateValue type
                    MessageCoding.DATE_STRING -> DateValue(DataEncoder.decodeDateString(bytes), bytes)
                    MessageCoding.DATE_TIME -> DateTimeValue(DataEncoder.decodeDateTime(bytes), bytes)
                    MessageCoding.DATE_TIME_STRING -> DateTimeValue(DataEncoder.decodeDateTimeString(bytes), bytes)
                    MessageCoding.MASKED_DATE -> MaskedDateValue(
                        DataEncoder.decodeMaskedDate(bytes),
                        bytes
                    )

                    MessageCoding.MRZ -> {
                        val unformattedMrz = DataEncoder.decodeC40(bytes)
                        val length = mrzLength ?: unformattedMrz.length
                        MrzValue(DataEncoder.formatMRZ(unformattedMrz, length), bytes)
                    }

                    MessageCoding.MRZ_MRVA -> {
                        val decoded = DataEncoder.decodeC40(bytes).replace(' ', '<')
                        MrzValue(decoded.take(44) + "\n" + decoded.drop(44).take(28), bytes)
                    }

                    MessageCoding.MRZ_MRVB -> {
                        val decoded = DataEncoder.decodeC40(bytes).replace(' ', '<')
                        MrzValue(decoded.take(36) + "\n" + decoded.drop(36).take(28), bytes)
                    }

                    MessageCoding.VALIDITY_DATES -> ValidityDatesValue.parse(bytes)
                    MessageCoding.DURATION_OF_STAY -> DurationOfStayValue.parse(bytes)
                    MessageCoding.BYTES, MessageCoding.SUB_MESSAGES, MessageCoding.UNKNOWN -> BytesValue(bytes)
                }
            } catch (e: Exception) {
                // If decoding fails, fall back to raw bytes
                BytesValue(bytes)
            }
        }
    }
}
