package de.tsenger.vdstools.generic

/**
 * A finding of the format validation of a seal (ICAO Doc 9303-13 Appendix D, step 1), without
 * judgement: the library reports, the application decides how to rate it. Signature, certificate
 * and issuer are not covered.
 */
sealed class FormatFinding {
    /** Coarse category following ICAO Doc 9303-13 Appendix D; stable when new finding types are added. */
    abstract val category: Category

    /**
     * Tags from the top level down to the affected message, e.g. `[0x01, 0x08]` for sub-tag 0x08
     * inside the IDB message 0x01. Empty for findings that concern no single tag.
     */
    abstract val tagPath: List<Int>

    enum class Category {
        /** A tag the definition does not know. 9303: does not affect validity if the signature is valid. */
        UNKNOWN_FEATURE,

        /** Structure or content violates the specification or profile. 9303: INVALID / WRONG_FORMAT. */
        WRONG_FORMAT,

        /** The format could not be checked, e.g. because the profile is unavailable. */
        UNDETERMINED,
    }

    /** A tag that neither the document type nor the profile defines; its message is raw ([Message.isRaw]). */
    data class UnknownTag(override val tagPath: List<Int>) : FormatFinding() {
        override val category get() = Category.UNKNOWN_FEATURE
    }

    /** A metadata tag the specification reserves without defining it, e.g. TR-03171 v0.9 tags 0x07–0x09. */
    data class ReservedTag(override val tagPath: List<Int>) : FormatFinding() {
        override val category get() = Category.UNKNOWN_FEATURE
    }

    /**
     * The document type is not known, so the content cannot be checked. [reference] is the VDS
     * document reference (feature reference and type category) or the IDB national document identifier.
     */
    data class UnknownDocumentType(val reference: Int) : FormatFinding() {
        override val category get() = Category.UNDETERMINED
        override val tagPath get() = emptyList<Int>()
    }

    /**
     * The TR-03171 document profile could not be resolved. [profileUuid] is the profile number as
     * uppercase hex (null if the seal has none), [profileUri] the seal's PROFILE_URI (null if absent).
     */
    data class ProfileNotFound(val profileUuid: String?, val profileUri: String?) : FormatFinding() {
        override val category get() = Category.UNDETERMINED
        override val tagPath get() = emptyList<Int>()
    }

    /**
     * The message or document type was interpreted with the national definition of another country
     * ([country]), because the seal's issuing country defines none and no international definition
     * exists, e.g. a German document type on a specimen seal issued by UTO. Empty [tagPath] for a VDS
     * document type.
     */
    data class ForeignNationalDefinition(override val tagPath: List<Int>, val country: String) : FormatFinding() {
        override val category get() = Category.UNDETERMINED
    }

    /** A message that the specification or profile requires is missing. */
    data class MissingRequired(override val tagPath: List<Int>, val name: String) : FormatFinding() {
        override val category get() = Category.WRONG_FORMAT
    }

    /** A message is present although the specification or profile forbids it. */
    data class NotAllowed(override val tagPath: List<Int>, val name: String) : FormatFinding() {
        override val category get() = Category.WRONG_FORMAT
    }

    /**
     * A message whose bytes cannot be decoded with the coding its definition prescribes, e.g. an
     * invalid date; its value is raw ([Message.isRaw]).
     */
    data class DecodeFailed(override val tagPath: List<Int>, val name: String) : FormatFinding() {
        override val category get() = Category.WRONG_FORMAT
    }

    /**
     * A message is shorter or longer than its definition allows. [length], [minLength] and
     * [maxLength] are characters for TR-03171 UTF8String profile entries and bytes otherwise;
     * a null bound is not specified.
     */
    data class InvalidLength(
        override val tagPath: List<Int>, val name: String, val length: Int, val minLength: Int?, val maxLength: Int?
    ) : FormatFinding() {
        override val category get() = Category.WRONG_FORMAT
    }

    /** The content of a container message cannot be split into its sub-messages. */
    data class MalformedSubMessages(override val tagPath: List<Int>, val name: String) : FormatFinding() {
        override val category get() = Category.WRONG_FORMAT
    }
}
