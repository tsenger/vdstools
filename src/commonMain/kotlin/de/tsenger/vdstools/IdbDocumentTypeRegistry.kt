package de.tsenger.vdstools

import de.tsenger.vdstools.internal.normalizeCountry
import de.tsenger.vdstools.internal.selectForCountry
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
 * Definitions are loaded from `IdbGermanDocumentTypes.json`.
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

    /**
     * Adds document types. An entry replaces any entry with the same name, and any entry with the same
     * tag **and** country.
     */
    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<IdbDocumentTypeDto>>(jsonString)
        for (dto in newDtos) {
            val country = dto.country?.let(::normalizeCountry)
            documentTypeDtoList = documentTypeDtoList.filter {
                it.name != dto.name && !(it.tag == dto.tag && it.country?.let(::normalizeCountry) == country)
            } + dto
        }
    }

    /**
     * The national document type for a `NATIONAL_DOCUMENT_IDENTIFIER` value read from a seal of
     * [issuingCountry]: that country's type first, then one without country, then the type of another
     * country if only one defines it. Null if none or ambiguous.
     */
    internal fun findDocumentType(tag: Int, issuingCountry: String?): IdbDocumentTypeDto? =
        selectForCountry(documentTypeDtoList.filter { it.tag == tag }, issuingCountry) { it.country }

    private fun findByName(name: String): IdbDocumentTypeDto? = documentTypeDtoList.lastOrNull { it.name == name }

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
    fun getDocumentType(tag: Int): String = getDocumentType(tag, null)

    /** As [getDocumentType], for a seal of [issuingCountry] (see [findDocumentType]). */
    fun getDocumentType(tag: Int, issuingCountry: String?): String {
        return findDocumentType(tag, issuingCountry)?.name ?: "UNKNOWN"
    }

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
    fun getExpectedMessages(tag: Int): List<IdbMessageTypeRef> = getExpectedMessages(tag, null)

    /** As [getExpectedMessages], for a seal of [issuingCountry] (see [findDocumentType]). */
    fun getExpectedMessages(tag: Int, issuingCountry: String?): List<IdbMessageTypeRef> {
        return findDocumentType(tag, issuingCountry)?.messages ?: emptyList()
    }

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