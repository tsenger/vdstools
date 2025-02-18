package de.tsenger.vdstools.idb.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdbDocumentTypeDto(
    val name: String,
    val tag: Int
)