package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.dto.IdbDocumentTypeDto
import kotlinx.serialization.json.Json

/**
 * Registry for IDB (ICAO Datastructure for Barcode) national document type definitions.
 *
 * This class loads document type definitions from a JSON configuration and provides
 * lookup functionality for document type names and tags.
 *
 * @param jsonString JSON string containing an array of [IdbDocumentTypeDto] definitions
 */
class IdbNationalDocumentTypeRegistry(jsonString: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var documentTypeDtoList: List<IdbDocumentTypeDto> = emptyList()
    private val documentTypes: HashMap<Int, IdbDocumentTypeDto> = HashMap()
    private val documentTypesInverse: HashMap<String, IdbDocumentTypeDto> = HashMap()


    init {
        val json = Json { ignoreUnknownKeys = true }
        documentTypeDtoList = json.decodeFromString(jsonString)
        for (documentTypeDto in documentTypeDtoList) {
            documentTypes[documentTypeDto.tag] = documentTypeDto
            documentTypesInverse[documentTypeDto.name] = documentTypeDto

        }
    }

    fun availableDocumentTypes(): Set<String> {
        return documentTypes.values.map { it.name }.toSet()
    }

    fun getDocumentType(tag: Int): String {
        return documentTypes[tag]?.name ?: "UNKNOWN"
    }

    fun getDocumentType(name: String): Int? {
        return documentTypesInverse[name]?.tag
    }

}