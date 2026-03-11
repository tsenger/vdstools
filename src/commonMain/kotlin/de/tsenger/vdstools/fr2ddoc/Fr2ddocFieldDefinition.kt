package de.tsenger.vdstools.fr2ddoc

import kotlinx.serialization.Serializable

@Serializable
data class Fr2ddocFieldDefinition(
    val perimeterId: String,
    val fieldId: String,
    val name: String,
    val minLength: Int,
    val maxLength: Int,
    val encoding: String
) {
    val isFixed: Boolean get() = minLength == maxLength && minLength > 0
}
