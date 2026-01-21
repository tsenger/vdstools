package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.idb.dto.IdbMessageTypeDto
import de.tsenger.vdstools.vds.MessageCoding
import kotlinx.serialization.json.Json

class IdbMessageTypeParser(jsonString: String) {
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
