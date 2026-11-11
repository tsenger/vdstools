package de.tsenger.vdstools

import de.tsenger.vdstools.internal.normalizeCountry
import de.tsenger.vdstools.internal.selectForCountry
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageDefinition
import de.tsenger.vdstools.generic.MessageDefinitionResolver
import de.tsenger.vdstools.idb.dto.IdbMessageTypeDto
import kotlinx.serialization.json.Json

/**
 * Registry for IDB message type definitions (ICAO TR-IDB / BSI TR-03137).
 *
 * IDB message types are the building blocks of an IDB message group. Each type defines a
 * specific piece of data (e.g., `FACE_IMAGE`, `MRZ_TD3`, `AZR`) identified by a numeric
 * tag and encoded according to a [de.tsenger.vdstools.generic.MessageCoding].
 *
 * Multiple message types can appear together in a message group. Which combination is
 * expected for a given document is determined by the national document type; see
 * [IdbDocumentTypeRegistry].
 *
 * The bundled definitions are loaded from `icao-idb/IdbMessageTypes.json` and `bsi-tr-03137/IdbMessageTypes.json`.
 *
 * @param jsonString JSON string containing an array of [IdbMessageTypeDto] definitions
 */
class IdbMessageTypeRegistry(jsonString: String) : DefinitionRegistry {
    private var messageTypeDtoList: List<IdbMessageTypeDto> = emptyList()
    private val messageTypesInverse: HashMap<String, IdbMessageTypeDto> = HashMap()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    init {
        addEntriesFromJson(jsonString)
    }

    /**
     * Adds message types. An entry replaces any entry with the same name, and any entry with the same
     * tag **and** country (an entry without country does not replace a national type of another name).
     */
    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<IdbMessageTypeDto>>(jsonString)
        for (dto in newDtos) {
            val country = dto.country?.let(::normalizeCountry)
            messageTypeDtoList = messageTypeDtoList.filter {
                it.name != dto.name && !(it.tag == dto.tag && it.country?.let(::normalizeCountry) == country)
            } + dto
        }
        messageTypesInverse.clear()
        messageTypeDtoList.forEach { messageTypesInverse[it.name] = it }
    }

    /**
     * The message type that carries the document types of [country] (`identifiesDocumentType`): that
     * country's own, else an international one. Unlike reading, no other country's type is used, since a
     * seal built with it would be read as a document type of that country.
     */
    internal fun findDocumentTypeIdentifier(country: String?): IdbMessageTypeDto? {
        val identifiers = messageTypeDtoList.filter { it.identifiesDocumentType }
        val normalized = country?.let(::normalizeCountry)
        return identifiers.lastOrNull { it.country?.let(::normalizeCountry) == normalized }
            ?: identifiers.lastOrNull { it.country == null }
    }

    /** Returns the names of all registered IDB message types. */
    fun availableMessageTypes(): Set<String> {
        return messageTypeDtoList.map { it.name }.toSet()
    }

    /**
     * Returns the message type name for a given tag.
     *
     * @param tag The numeric tag of the message type
     * @return The message type name, or `"UNKNOWN"` if the tag is not registered
     */
    fun getMessageType(tag: Int): String {
        return getMessageTypeDto(tag)?.name ?: "UNKNOWN"
    }

    /**
     * Returns the numeric tag for a given message type name.
     *
     * @param messageTypeName The message type name (e.g., `"FACE_IMAGE"`)
     * @return The tag value, or `null` if the name is not registered
     */
    fun getMessageType(messageTypeName: String): Int? {
        return findByName(messageTypeName)?.tag
    }

    /**
     * Returns the encoding for a given message type name.
     *
     * @param messageTypeName The message type name (e.g., `"MRZ_TD3"`)
     * @return The [MessageCoding], or [MessageCoding.UNKNOWN] if the name is not registered
     */
    fun getMessageTypeCoding(messageTypeName: String): MessageCoding {
        return findByName(messageTypeName)?.coding ?: MessageCoding.UNKNOWN
    }

    /**
     * Returns the encoding for a given message type tag.
     *
     * @param messageTypeTag The numeric tag of the message type
     * @return The [MessageCoding], or [MessageCoding.UNKNOWN] if the tag is not registered
     */
    fun getMessageTypeCoding(messageTypeTag: Int): MessageCoding {
        return getMessageTypeDto(messageTypeTag)?.coding ?: MessageCoding.UNKNOWN
    }

    /**
     * Returns the full DTO for a given message type tag.
     *
     * @param tag The numeric tag of the message type
     * @return The [IdbMessageTypeDto], or `null` if the tag is not registered
     */
    fun getMessageTypeDto(tag: Int): IdbMessageTypeDto? = getMessageTypeDto(tag, null)

    /**
     * Returns the message type for a tag read from a seal of [issuingCountry]: the national type of that
     * country first, then an ICAO type, then the national type of another country if only one defines it.
     *
     * @return The [IdbMessageTypeDto], or `null` if the tag is not registered or several other countries define it
     */
    fun getMessageTypeDto(tag: Int, issuingCountry: String?): IdbMessageTypeDto? =
        selectForCountry(messageTypeDtoList.filter { it.tag == tag }, issuingCountry) { it.country }

    /**
     * Returns the full DTO for a given message type name.
     *
     * @param name The message type name (e.g., `"PROOF_OF_VACCINATION"`)
     * @return The [IdbMessageTypeDto], or `null` if the name is not registered
     */
    fun getMessageTypeDto(name: String): IdbMessageTypeDto? {
        return findByName(name)
    }

    // exact match first, then ignoring case (consistent with getMessageByName)
    private fun findByName(name: String): IdbMessageTypeDto? =
        messageTypesInverse[name]
            ?: messageTypesInverse.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    fun asResolver(): MessageDefinitionResolver = object : MessageDefinitionResolver {
        override fun resolveByTag(tag: String): MessageDefinition? {
            val tagInt = tag.toIntOrNull(16) ?: return null
            val dto = getMessageTypeDto(tagInt) ?: return null
            return MessageDefinition(tag.uppercase().padStart(2, '0'), dto.name, dto.coding)
        }

        override fun resolveByName(name: String): MessageDefinition? {
            val dto = getMessageTypeDto(name) ?: return null
            val tagHex = (dto.tag and 0xFF).toString(16).uppercase().padStart(2, '0')
            return MessageDefinition(tagHex, dto.name, dto.coding)
        }
    }

}
