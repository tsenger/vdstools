package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.generated.ResourceConstants
import kotlinx.serialization.json.Json

object Fr2ddocFieldRegistry {

    private val definitions: Map<String, Fr2ddocFieldDefinition>

    init {
        val json = Json { ignoreUnknownKeys = true }
        val list = json.decodeFromString<List<Fr2ddocFieldDefinition>>(
            ResourceConstants.FR2DDOC_FIELD_DEFINITIONS_JSON
        )
        definitions = list.associateBy { "${it.perimeterId}:${it.fieldId}" }
    }

    fun getDefinition(perimeterId: String, fieldId: String): Fr2ddocFieldDefinition? {
        // Try exact match first, then with leading-zero-stripped perimeter ID
        return definitions["$perimeterId:$fieldId"]
            ?: definitions["${perimeterId.trimStart('0').ifEmpty { "0" }}:$fieldId"]
    }
}
