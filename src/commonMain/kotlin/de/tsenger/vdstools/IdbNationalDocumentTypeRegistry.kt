package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.dto.IdbDocumentTypeDto
import de.tsenger.vdstools.idb.dto.IdbMessageTypeRef
import kotlinx.serialization.json.Json

/**
 * Registry for IDB national document type definitions (ICAO TR-IDB / BSI TR-03137).
 *
 * In IDB, a message group can contain multiple message types belonging to one document.
 * Which message types are expected is determined by the national document type, encoded
 * as `NATIONAL_DOCUMENT_IDENTIFIER` (tag 0x86) in the message group. This registry maps
 * between the numeric tag and the document type name, and provides the list of expected
 * message types for each national document type.
 *
 * Definitions are loaded from `IdbNationalDocumentTypes.json`.
 *
 * @param jsonString JSON string containing an array of [IdbDocumentTypeDto] definitions
 */
class IdbNationalDocumentTypeRegistry(jsonString: String) : DefinitionRegistry {
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

    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = Json { ignoreUnknownKeys = true }.decodeFromString<List<IdbDocumentTypeDto>>(jsonString)
        for (dto in newDtos) {
            documentTypes[dto.tag] = dto
            documentTypesInverse[dto.name] = dto
        }
    }

    /** Returns the names of all registered national document types. */
    fun availableDocumentTypes(): Set<String> {
        return documentTypes.values.map { it.name }.toSet()
    }

    /**
     * Returns the document type name for a given national document type tag.
     *
     * @param tag The numeric tag value of the `NATIONAL_DOCUMENT_IDENTIFIER` message
     * @return The document type name, or `"UNKNOWN"` if the tag is not registered
     */
    fun getDocumentType(tag: Int): String {
        return documentTypes[tag]?.name ?: "UNKNOWN"
    }

    /**
     * Returns the numeric tag for a given national document type name.
     *
     * @param name The document type name (e.g., `"SUBSTITUTE_IDENTITY_DOCUMENT"`)
     * @return The tag value, or `null` if the name is not registered
     */
    fun getDocumentType(name: String): Int? {
        return documentTypesInverse[name]?.tag
    }

    /**
     * Returns the IDB message types expected in the message group for a given national document type.
     *
     * The returned list contains references to entries in `IdbMessageTypes.json`, identified by name.
     * Use [DataEncoder.getIdbMessageTypeTag] or [DataEncoder.getIdbMessageTypeCoding] to resolve them.
     *
     * @param tag The numeric tag value of the `NATIONAL_DOCUMENT_IDENTIFIER` message
     * @return List of expected message type references, empty if the tag is not registered or has no messages defined
     */
    fun getExpectedMessages(tag: Int): List<IdbMessageTypeRef> {
        return documentTypes[tag]?.messages ?: emptyList()
    }

    /**
     * Returns the IDB message types expected in the message group for a given national document type name.
     *
     * @param name The document type name (e.g., `"SUBSTITUTE_IDENTITY_DOCUMENT"`)
     * @return List of expected message type references, empty if the name is not registered or has no messages defined
     */
    fun getExpectedMessages(name: String): List<IdbMessageTypeRef> {
        return documentTypesInverse[name]?.messages ?: emptyList()
    }

}