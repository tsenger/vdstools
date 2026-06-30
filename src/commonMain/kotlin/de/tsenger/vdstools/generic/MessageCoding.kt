package de.tsenger.vdstools.generic

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
enum class MessageCoding {
    C40,
    UTF8_STRING,
    BYTES,
    SUB_MESSAGES,
    BYTE,

    /**
     * Signed integer encoded as minimal-length big-endian two's-complement bytes (the content
     * octets of an ASN.1 INTEGER). Used by BSI TR-03171 profile fields of ASN.1 type `INTEGER`,
     * independent of the field's `length` (which is a validation constraint, not a coding choice).
     *
     * Accepts [Int], [Long] or a decimal [String] as input. Decodes to [MessageValue.IntegerValue].
     */
    INTEGER,

    MASKED_DATE,
    DATE,
    DATE_TIME,

    /**
     * Standard MRZ encoding using C40, with no predefined line length.
     *
     * The line split is inferred from the decoded string length (half the total). Use for TD1
     * (90 chars), TD2 (66 chars), TD3 (88 chars), and other formats where the full MRZ is stored.
     */
    MRZ,

    /**
     * MRZ encoding for MRV-A visas as specified by ICAO 9303 / VDS-NC.
     *
     * Stores only line 1 (44 chars) and the first 28 characters of line 2, totalling 72 chars.
     * The optional data field (positions 29–44 of line 2) is intentionally omitted to reduce
     * the barcode payload size.
     *
     * Encoding input: the full MRZ string (with or without newline separator). Newlines are
     * stripped before truncation.
     * Decoding output: line 1 (44 chars) + newline + line 2 (up to 28 chars), no padding.
     */
    MRZ_MRVA,

    /**
     * MRZ encoding for MRV-B visas as specified by ICAO 9303 / VDS-NC.
     *
     * Stores only line 1 (36 chars) and the first 28 characters of line 2, totalling 64 chars.
     * The optional data field (positions 29–36 of line 2) is intentionally omitted to reduce
     * the barcode payload size.
     *
     * Encoding input: the full MRZ string (with or without newline separator). Newlines are
     * stripped before truncation.
     * Decoding output: line 1 (36 chars) + newline + line 2 (up to 28 chars), no padding.
     */
    MRZ_MRVB,

    VALIDITY_DATES,

    /**
     * Date encoding as an 8-byte ASCII/UTF-8 string in `YYYYMMDD` order, as specified by
     * BSI TR-03171 version 0.9 for the `validFrom` (tag 0x01) and `validTo` (tag 0x02) fields
     * in the message zone of administrative documents (document category 0xC9).
     *
     * This is distinct from [DATE], which uses a 3-byte binary encoding as defined by ICAO Doc 9303
     * Part 13 and is used for standard VDS-NC document dates.
     *
     * Accepts [kotlinx.datetime.LocalDate] or an ISO-8601 string (`yyyy-MM-dd`) as input.
     * Decodes to [MessageValue.DateValue].
     */
    DATE_STRING,

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
