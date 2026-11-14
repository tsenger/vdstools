package de.tsenger.vdstools

import de.tsenger.vdstools.idb.dto.IdbDocumentTypeDto
import de.tsenger.vdstools.idb.dto.IdbMessageTypeRef
import kotlinx.serialization.json.Json

/**
 * Registry for IDB national document type definitions (ICAO TR-IDB / BSI TR-03137).
 *
 * In IDB, a message group can contain multiple message types belonging to one document.
 * Which message types are expected is determined by the national document type, the value of the
 * message type marked `identifiesDocumentType` (BSI TR-03137: `NATIONAL_DOCUMENT_IDENTIFIER`, tag 0x86). This registry maps
 * between the numeric tag and the document type name, and provides the list of expected
 * message types for each national document type.
 *
 * The bundled definitions are loaded from `bsi-tr-03137/IdbDocumentTypes.json`.
 *
 * @param jsonString JSON string containing an array of [IdbDocumentTypeDto] definitions
 */
class IdbDocumentTypeRegistry(jsonString: String) : DefinitionRegistry {
    private var documentTypeDtoList: List<IdbDocumentTypeDto> = emptyList()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    init {
        addEntriesFromJson(jsonString)
    }

    /** Adds document types. An entry replaces any entry with the same name or the same tag. */
    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<IdbDocumentTypeDto>>(jsonString)
        for (dto in newDtos) {
            documentTypeDtoList = documentTypeDtoList.filter { it.name != dto.name && it.tag != dto.tag } + dto
        }
    }

    /** The national document type for a document type identifier value, null if not registered. */
    internal fun findDocumentType(tag: Int): IdbDocumentTypeDto? = documentTypeDtoList.lastOrNull { it.tag == tag }

    internal fun findByName(name: String): IdbDocumentTypeDto? = documentTypeDtoList.lastOrNull { it.name == name }

    /** Returns the names of all registered national document types. */
    fun availableDocumentTypes(): Set<String> {
        return documentTypeDtoList.map { it.name }.toSet()
    }

    /**
     * Returns the document type name for a given national document type tag.
     *
     * @param tag The numeric tag value of the `NATIONAL_DOCUMENT_IDENTIFIER` message
     * @return The document type name, or `"UNKNOWN"` if the tag is not registered
     */
    fun getDocumentType(tag: Int): String = findDocumentType(tag)?.name ?: "UNKNOWN"

    /**
     * Returns the numeric tag for a given national document type name.
     *
     * @param name The document type name (e.g., `"SUBSTITUTE_IDENTITY_DOCUMENT"`)
     * @return The tag value, or `null` if the name is not registered
     */
    fun getDocumentType(name: String): Int? {
        return findByName(name)?.tag
    }

    /**
     * Returns the IDB message types expected in the message group for a given national document type.
     *
     * The returned list contains references to entries in `IdbMessageTypes.json`, identified by name.
     * Use [IdbMessageTypeRegistry.getMessageType] or [IdbMessageTypeRegistry.getMessageTypeCoding] to resolve them.
     *
     * @param tag The numeric tag value of the `NATIONAL_DOCUMENT_IDENTIFIER` message
     * @return List of expected message type references, empty if the tag is not registered or has no messages defined
     */
    fun getExpectedMessages(tag: Int): List<IdbMessageTypeRef> = findDocumentType(tag)?.messages ?: emptyList()

    /**
     * Returns the IDB message types expected in the message group for a given national document type name.
     *
     * @param name The document type name (e.g., `"SUBSTITUTE_IDENTITY_DOCUMENT"`)
     * @return List of expected message type references, empty if the name is not registered or has no messages defined
     */
    fun getExpectedMessages(name: String): List<IdbMessageTypeRef> {
        return findByName(name)?.messages ?: emptyList()
    }

}