package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.dto.IdbMessageTypeDto
import de.tsenger.vdstools.generic.MessageCoding
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
 * Definitions are loaded from `IdbMessageTypes.json`.
 *
 * @param jsonString JSON string containing an array of [IdbMessageTypeDto] definitions
 */
class IdbMessageTypeRegistry(jsonString: String) : DefinitionRegistry {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var messageTypeDtoList: List<IdbMessageTypeDto> = emptyList()
    private val messageTypes: HashMap<Int, IdbMessageTypeDto> = HashMap()
    private val messageTypesInverse: HashMap<String, IdbMessageTypeDto> = HashMap()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    init {
        messageTypeDtoList = json.decodeFromString(jsonString)
        for (messageTypeDto in messageTypeDtoList) {
            messageTypes[messageTypeDto.tag] = messageTypeDto
            messageTypesInverse[messageTypeDto.name] = messageTypeDto

        }
    }

    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<IdbMessageTypeDto>>(jsonString)
        for (dto in newDtos) {
            messageTypes[dto.tag] = dto
            messageTypesInverse[dto.name] = dto
        }
    }

    /** Returns the names of all registered IDB message types. */
    fun availableMessageTypes(): Set<String> {
        return messageTypes.values.map { it.name }.toSet()
    }

    /**
     * Returns the message type name for a given tag.
     *
     * @param tag The numeric tag of the message type
     * @return The message type name, or `"UNKNOWN"` if the tag is not registered
     */
    fun getMessageType(tag: Int): String {
        return messageTypes[tag]?.name ?: "UNKNOWN"
    }

    /**
     * Returns the numeric tag for a given message type name.
     *
     * @param messageTypeName The message type name (e.g., `"FACE_IMAGE"`)
     * @return The tag value, or `null` if the name is not registered
     */
    fun getMessageType(messageTypeName: String): Int? {
        return messageTypesInverse[messageTypeName]?.tag
    }

    /**
     * Returns the encoding for a given message type name.
     *
     * @param messageTypeName The message type name (e.g., `"MRZ_TD3"`)
     * @return The [MessageCoding], or [MessageCoding.UNKNOWN] if the name is not registered
     */
    fun getMessageTypeCoding(messageTypeName: String): MessageCoding {
        return messageTypesInverse[messageTypeName]?.coding ?: MessageCoding.UNKNOWN
    }

    /**
     * Returns the encoding for a given message type tag.
     *
     * @param messageTypeTag The numeric tag of the message type
     * @return The [MessageCoding], or [MessageCoding.UNKNOWN] if the tag is not registered
     */
    fun getMessageTypeCoding(messageTypeTag: Int): MessageCoding {
        return messageTypes[messageTypeTag]?.coding ?: MessageCoding.UNKNOWN
    }

    /**
     * Returns the full DTO for a given message type tag.
     *
     * @param tag The numeric tag of the message type
     * @return The [IdbMessageTypeDto], or `null` if the tag is not registered
     */
    fun getMessageTypeDto(tag: Int): IdbMessageTypeDto? {
        return messageTypes[tag]
    }

    /**
     * Returns the full DTO for a given message type name.
     *
     * @param name The message type name (e.g., `"PROOF_OF_VACCINATION"`)
     * @return The [IdbMessageTypeDto], or `null` if the name is not registered
     */
    fun getMessageTypeDto(name: String): IdbMessageTypeDto? {
        return messageTypesInverse[name]
    }

}
