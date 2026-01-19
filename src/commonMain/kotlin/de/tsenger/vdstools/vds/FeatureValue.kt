package de.tsenger.vdstools.vds

import kotlinx.datetime.LocalDate

sealed class FeatureValue {
    abstract val rawBytes: ByteArray
    abstract val decoded: Any

    override fun toString(): String = decoded.toString()

    data class ByteValue(val value: Int, override val rawBytes: ByteArray) : FeatureValue() {
        override val decoded: Int get() = value

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByteValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value + rawBytes.contentHashCode()
    }

    data class StringValue(val value: String, override val rawBytes: ByteArray) : FeatureValue() {
        override val decoded: String get() = value

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
    }

    data class DateValue(val date: LocalDate, override val rawBytes: ByteArray) : FeatureValue() {
        override val decoded: LocalDate get() = date

        override fun toString(): String = decoded.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DateValue) return false
            return date == other.date && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * date.hashCode() + rawBytes.contentHashCode()
    }

    data class MaskedDateValue(val value: String, override val rawBytes: ByteArray) : FeatureValue() {
        override val decoded: String get() = value

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MaskedDateValue) return false
            return value == other.value && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * value.hashCode() + rawBytes.contentHashCode()
    }

    data class MrzValue(val mrz: String, override val rawBytes: ByteArray) : FeatureValue() {
        override val decoded: String get() = mrz

        override fun toString(): String = decoded

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MrzValue) return false
            return mrz == other.mrz && rawBytes.contentEquals(other.rawBytes)
        }

        override fun hashCode(): Int = 31 * mrz.hashCode() + rawBytes.contentHashCode()
    }

    @OptIn(ExperimentalStdlibApi::class)
    data class BytesValue(override val rawBytes: ByteArray) : FeatureValue() {
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
         * Factory method: Creates FeatureValue based on coding type.
         * @param bytes The raw byte array to decode
         * @param coding The encoding type of the bytes
         * @param mrzLength Optional MRZ length for correct formatting (88 for MRVA, 72 for MRVB)
         */
        fun fromBytes(bytes: ByteArray, coding: FeatureCoding, mrzLength: Int? = null): FeatureValue {
            return try {
                when (coding) {
                    FeatureCoding.BYTE -> {
                        if (bytes.isEmpty()) BytesValue(bytes)
                        else ByteValue(bytes[0].toInt() and 0xFF, bytes)
                    }
                    FeatureCoding.C40 -> StringValue(de.tsenger.vdstools.DataEncoder.decodeC40(bytes), bytes)
                    FeatureCoding.UTF8_STRING -> StringValue(bytes.decodeToString(), bytes)
                    FeatureCoding.DATE -> DateValue(de.tsenger.vdstools.DataEncoder.decodeDate(bytes), bytes)
                    FeatureCoding.MASKED_DATE -> MaskedDateValue(
                        de.tsenger.vdstools.DataEncoder.decodeMaskedDate(bytes),
                        bytes
                    )

                    FeatureCoding.MRZ -> {
                        val unformattedMrz = de.tsenger.vdstools.DataEncoder.decodeC40(bytes)
                        val length = mrzLength ?: unformattedMrz.length
                        MrzValue(de.tsenger.vdstools.DataEncoder.formatMRZ(unformattedMrz, length), bytes)
                    }

                    FeatureCoding.BYTES, FeatureCoding.UNKNOWN -> BytesValue(bytes)
                }
            } catch (e: Exception) {
                // If decoding fails, fall back to raw bytes
                BytesValue(bytes)
            }
        }
    }
}
