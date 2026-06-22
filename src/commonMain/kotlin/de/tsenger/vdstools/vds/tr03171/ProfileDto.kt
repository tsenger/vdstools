package de.tsenger.vdstools.vds.tr03171

/**
 * Parsed representation of a TR-03171 v0.9 document profile (the `<profile>` XML element).
 *
 * The metadata fields below are *not* encoded into the seal itself; they describe and
 * identify the profile. Only [profileNumber] additionally appears in the seal (Tag 0x00 of
 * the message zone). The actual seal content fields are described by [entries] (tags 0x0A–0xFE).
 */
data class ProfileDto(
    /** UUIDv4 (32 uppercase hex chars, no dashes) uniquely identifying the profile. Mandatory. */
    val profileNumber: String,
    /** Version of the Technical Guideline the profile conforms to (`versionTR`). Mandatory. */
    val versionTR: String,
    /** Human-readable profile name. Optional. */
    val profileName: String? = null,
    /** Organisational unit responsible for the profile. Optional. */
    val creator: String? = null,
    /** Free-text classification of the profile. Optional. */
    val category: String? = null,
    /** One or more 14-digit LeiKa IDs separated by `;`. Optional. */
    val leikaID: String? = null,
    /** Whether the validity start date (Tag 0x01) must be present in the seal. Mandatory. */
    val validFromPresent: Boolean,
    /** Whether the validity end date (Tag 0x02) must be present in the seal. Mandatory. */
    val validToPresent: Boolean,
    /** The seal's content fields (tags 0x0A–0xFE). */
    val entries: List<ProfileEntryDto>
)
