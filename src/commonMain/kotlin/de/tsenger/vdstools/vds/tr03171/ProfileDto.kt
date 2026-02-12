package de.tsenger.vdstools.vds.tr03171

data class ProfileDto(
    val profileNumber: String,
    val profileName: String,
    val creator: String,
    val category: String? = null,
    val leikaID: String? = null,
    val statusIndicator: StatusIndicator? = null,
    val entries: List<ProfileEntryDto>
)
