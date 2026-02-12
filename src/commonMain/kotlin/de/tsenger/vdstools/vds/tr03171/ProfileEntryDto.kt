package de.tsenger.vdstools.vds.tr03171

data class ProfileEntryDto(
    val tag: Int,
    val optional: Boolean = false,
    val name: String,
    val description: String,
    val length: Int? = null,
    val type: Asn1Type,
    val defaultValue: String? = null
)
