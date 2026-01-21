package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.MessageCoding
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import de.tsenger.vdstools.vds.dto.MessageDto
import de.tsenger.vdstools.vds.dto.SealDto
import kotlinx.serialization.json.Json


class MessageConverter(jsonString: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var sealDtoList: List<SealDto>

    private val vdsTypes: MutableMap<String, Int> = HashMap()
    private val vdsTypesReverse: MutableMap<Int, String> = HashMap()
    private val vdsMessages: MutableSet<String> = mutableSetOf()


    init {
        val json = Json { ignoreUnknownKeys = true }
        this.sealDtoList = json.decodeFromString(jsonString)
        populateMappings()
    }

    private fun populateMappings() {
        for ((documentType, documentRef, _, messages) in sealDtoList) {
            if (documentType != "" && documentRef != "") {
                vdsTypes[documentType] = documentRef.toInt(16)
                vdsTypesReverse[documentRef.toInt(16)] = documentType
            }
            messages.forEach { vdsMessages.add(it.name) }
        }
    }

    val availableVdsTypes: List<String>
        get() = vdsTypes.keys.toList()

    fun getDocumentRef(vdsType: String): Int? {
        return vdsTypes[vdsType]
    }

    fun getVdsType(docRef: Int): String? {
        return vdsTypesReverse[docRef]
    }

    /**
     * Checks if the given vdsType requires UUID-based profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return true if this type requires UUID lookup, false otherwise
     */
    fun requiresUuidLookup(vdsType: String): Boolean {
        return try {
            val sealDto = getSealDto(vdsType)
            sealDto.uuidMessageLookup
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Gets the tag number containing the UUID for profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return The tag number (default 0 if not specified or type not found)
     */
    fun getUuidMessageTag(vdsType: String): Int {
        return try {
            val sealDto = getSealDto(vdsType)
            sealDto.uuidMessageTag
        } catch (_: IllegalArgumentException) {
            0
        }
    }

    val availableVdsMessages: Set<String?>
        get() = vdsMessages

    @Throws(IllegalArgumentException::class)
    fun getMessageName(vdsType: String, derTlv: DerTlv): String {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return getMessageName(sealDto, derTlv.tag.toInt())
    }

    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(vdsType: String, derTlv: DerTlv): MessageCoding {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        val tag = derTlv.tag
        return getCoding(sealDto, tag)
    }

    /**
     * Gets the message name for a given tag, considering extended message definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended message definition (can be null)
     * @param tag The tag number to look up
     * @return The message name
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageName(baseVdsType: String, extendedDefinition: ExtendedMessageDefinitionDto?, tag: Int): String {
        // Try extended definition first if available
        if (extendedDefinition != null) {
            val definitionMessage = extendedDefinition.messages.find { it.tag == tag }
            if (definitionMessage != null) {
                return definitionMessage.name
            }
        }
        // Fall back to base type
        val sealDto = getSealDto(baseVdsType)
        return getMessageName(sealDto, tag)
    }

    /**
     * Gets the message coding for a given tag, considering extended message definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended message definition (can be null)
     * @param tag The tag number to look up
     * @return The message coding
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(
        baseVdsType: String,
        extendedDefinition: ExtendedMessageDefinitionDto?,
        tag: Int
    ): MessageCoding {
        // Try extended definition first if available
        if (extendedDefinition != null) {
            val definitionMessage = extendedDefinition.messages.find { it.tag == tag }
            if (definitionMessage != null) {
                return definitionMessage.coding
            }
        }
        // Fall back to base type
        val sealDto = getSealDto(baseVdsType)
        return getCoding(sealDto, tag.toByte())
    }


    @Throws(IllegalArgumentException::class)
    fun <T> encodeMessage(vdsType: String, messageName: String, inputValue: T): DerTlv {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No VdsSeal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        if (!vdsMessages.contains(messageName)) {
            log.w("No VdsSeal message with name '$messageName' was found.")
            throw IllegalArgumentException("No VdsSeal message with name '$messageName' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return encodeMessage(sealDto, messageName, inputValue)
    }

    @Throws(IllegalArgumentException::class)
    private fun <T> encodeMessage(sealDto: SealDto, messageName: String, inputValue: T): DerTlv {
        val tag = getMessageTag(sealDto, messageName)
        if (tag.toInt() == 0) {
            log.w("VdsType: " + sealDto.documentType + " has no Message " + messageName)
            throw IllegalArgumentException("VdsType: " + sealDto.documentType + " has no Message " + messageName)
        }
        val coding = getCoding(sealDto, messageName)
        val value = DataEncoder.encodeValueByCoding(coding, inputValue)
        return DerTlv(tag, value)
    }


    @Throws(IllegalArgumentException::class)
    fun getMessageTag(vdsType: String, messageName: String): Int {
        val sealDto = getSealDto(vdsType)
        return getMessageTag(sealDto, messageName).toInt()
    }

    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(vdsType: String, tag: Int): MessageCoding {
        val sealDto = getSealDto(vdsType)
        return getCoding(sealDto, tag.toByte())
    }

    @Throws(IllegalArgumentException::class)
    private fun getMessageTag(sealDto: SealDto, message: String): Byte {
        for ((name, tag) in sealDto.messages) {
            if (name.equals(message, ignoreCase = true)) {
                return tag.toByte()
            }
        }
        throw IllegalArgumentException("Message '" + message + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getMessageName(sealDto: SealDto, tag: Int): String {
        for ((name, tag1) in sealDto.messages) {
            if (tag1 == tag) {
                return name
            }
        }
        throw IllegalArgumentException("No Message with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, message: String): MessageCoding {
        for ((name, _, coding) in sealDto.messages) {
            if (name.equals(message, ignoreCase = true)) {
                return coding
            }
        }
        throw IllegalArgumentException("Message '" + message + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, tag: Byte): MessageCoding {
        for ((_, tag1, coding) in sealDto.messages) {
            if (tag1 == tag.toInt()) {
                return coding
            }
        }
        throw IllegalArgumentException("No Message with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getMessageDto(sealDto: SealDto, tag: Byte): MessageDto {
        for (messageDto in sealDto.messages) {
            if (messageDto.tag == tag.toInt()) {
                return messageDto
            }
        }
        throw IllegalArgumentException("No Message with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getSealDto(vdsType: String): SealDto {
        for (sealDto in sealDtoList) {
            if (sealDto.documentType == vdsType) {
                return sealDto
            }
        }
        throw IllegalArgumentException("VdsType '$vdsType' is unspecified in SealCodings.")
    }


}
