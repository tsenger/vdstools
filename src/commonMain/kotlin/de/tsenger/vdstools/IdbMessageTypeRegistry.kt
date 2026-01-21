package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.dto.IdbMessageTypeDto
import de.tsenger.vdstools.generic.MessageCoding
import kotlinx.serialization.json.Json

/**
 * Registry for IDB (ICAO Datastructure for Barcode) message type definitions.
 *
 * This class loads message type definitions from a JSON configuration and provides
 * lookup functionality for message type names, tags, and codings.
 *
 * @param jsonString JSON string containing an array of [IdbMessageTypeDto] definitions
 */
class IdbMessageTypeRegistry(jsonString: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var messageTypeDtoList: List<IdbMessageTypeDto> = emptyList()
    private val messageTypes: HashMap<Int, IdbMessageTypeDto> = HashMap()
    private val messageTypesInverse: HashMap<String, IdbMessageTypeDto> = HashMap()


    init {
        val json = Json { ignoreUnknownKeys = true }
        messageTypeDtoList = json.decodeFromString(jsonString)
        for (messageTypeDto in messageTypeDtoList) {
            messageTypes[messageTypeDto.tag] = messageTypeDto
            messageTypesInverse[messageTypeDto.name] = messageTypeDto

        }
    }

    fun availableMessageTypes(): Set<String> {
        return messageTypes.values.map { it.name }.toSet()
    }

    fun getMessageType(tag: Int): String {
        return messageTypes[tag]?.name ?: "UNKNOWN"
    }

    fun getMessageType(messageTypeName: String): Int? {
        return messageTypesInverse[messageTypeName]?.tag
    }

    fun getMessageTypeCoding(messageTypeName: String): MessageCoding {
        return messageTypesInverse[messageTypeName]?.coding ?: MessageCoding.UNKNOWN
    }

    fun getMessageTypeCoding(messageTypeTag: Int): MessageCoding {
        return messageTypes[messageTypeTag]?.coding ?: MessageCoding.UNKNOWN
    }

}
