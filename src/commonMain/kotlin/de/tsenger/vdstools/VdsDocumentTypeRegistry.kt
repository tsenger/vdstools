package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.dto.VdsDocumentTypeDto
import kotlinx.serialization.json.Json


/**
 * Central component for processing VDS (Visible Digital Seal) message definitions.
 *
 * This class loads seal definitions from a JSON configuration and provides functionality for:
 * - Bidirectional mapping between VDS type names and their hexadecimal document references
 * - Encoding values into DER-TLV format based on type and message specifications
 * - Decoding messages by resolving tag numbers to message names and codings
 * - Supporting profile definitions with fallback to base types
 * - UUID-based profile lookup for certain VDS types
 *
 * @param jsonString JSON string containing an array of [VdsDocumentTypeDto] definitions
 */
class VdsDocumentTypeRegistry(jsonString: String) : DefinitionRegistry {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var documentTypeDtoList: List<VdsDocumentTypeDto>

    private val vdsTypes: MutableMap<String, Int> = HashMap()
    private val vdsTypesReverse: MutableMap<Int, String> = HashMap()
    private val vdsMessages: MutableSet<String> = mutableSetOf()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    init {
        this.documentTypeDtoList = json.decodeFromString(jsonString)
        populateMappings()
    }

    private fun populateMappings() {
        for ((documentType, documentRef, _, messages) in documentTypeDtoList) {
            if (documentType != "" && documentRef != "") {
                vdsTypes[documentType] = documentRef.toInt(16)
                vdsTypesReverse[documentRef.toInt(16)] = documentType
            }
            messages.forEach { vdsMessages.add(it.name) }
        }
    }

    /**
     * Returns a list of all available VDS type names.
     */
    val availableVdsTypes: List<String>
        get() = vdsTypes.keys.toList()

    /**
     * Returns the document reference number for a given VDS type name.
     *
     * @param vdsType The VDS type name (e.g., "ARRIVAL_ATTESTATION")
     * @return The document reference as integer, or null if the type is not found
     */
    fun getDocumentRef(vdsType: String): Int? {
        return vdsTypes[vdsType]
    }

    /**
     * Returns the VDS type name for a given document reference number.
     *
     * @param docRef The document reference number
     * @return The VDS type name, or null if the reference is not found
     */
    fun getVdsType(docRef: Int): String? {
        return vdsTypesReverse[docRef]
    }

    /**
     * Checks if the given vdsType requires UUID-based profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return true if this type requires UUID lookup, false otherwise
     */
    fun requiresProfileLookup(vdsType: String): Boolean {
        return try {
            val docTypeDto = getVdsDocumentTypeDto(vdsType)
            docTypeDto.uuidMessageLookup
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
            val docTypeDto = getVdsDocumentTypeDto(vdsType)
            docTypeDto.uuidMessageTag
        } catch (_: IllegalArgumentException) {
            0
        }
    }

    /**
     * Returns the set of tag numbers that are treated as metadata for the given VDS type.
     *
     * @param vdsType The VDS type to check
     * @return Set of metadata tag numbers, empty if not configured or type not found
     */
    fun getMetadataTags(vdsType: String): Set<Int> {
        return try {
            getVdsDocumentTypeDto(vdsType).metadataTagList.toSet()
        } catch (_: IllegalArgumentException) {
            emptySet()
        }
    }

    /**
     * Returns a set of all available message names across all VDS types.
     */
    val availableVdsMessages: Set<String?>
        get() = vdsMessages

    /**
     * Returns the message name for a given VDS type and DER-TLV structure.
     *
     * @param vdsType The VDS type name
     * @param derTlv The DER-TLV structure containing the tag to look up
     * @return The message name corresponding to the tag
     * @throws IllegalArgumentException if the VDS type or tag is not found
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageName(vdsType: String, derTlv: DerTlv): String {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val docTypeDto = getVdsDocumentTypeDto(vdsType)
        return getMessageName(docTypeDto, derTlv.tag.toInt())
    }

    /**
     * Returns the message coding for a given VDS type and DER-TLV structure.
     *
     * @param vdsType The VDS type name
     * @param derTlv The DER-TLV structure containing the tag to look up
     * @return The [MessageCoding] for the specified tag
     * @throws IllegalArgumentException if the VDS type or tag is not found
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(vdsType: String, derTlv: DerTlv): MessageCoding {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val docTypeDto = getVdsDocumentTypeDto(vdsType)
        val tag = derTlv.tag
        return getCoding(docTypeDto, tag)
    }

    /**
     * Gets the message name for a given tag, considering profile definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param profileDefinition The resolved profile definition (can be null)
     * @param tag The tag number to look up
     * @return The message name
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageName(baseVdsType: String, profileDefinition: VdsProfileDefinitionDto?, tag: Int): String {
        // Try extended definition first if available
        if (profileDefinition != null) {
            val definitionMessage = profileDefinition.messages.find { it.tag == tag }
            if (definitionMessage != null) {
                return definitionMessage.name
            }
        }
        // Fall back to base type
        val docTypeDto = getVdsDocumentTypeDto(baseVdsType)
        return getMessageName(docTypeDto, tag)
    }

    /**
     * Gets the message coding for a given tag, considering profile definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param profileDefinition The resolved profile definition (can be null)
     * @param tag The tag number to look up
     * @return The message coding
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(
        baseVdsType: String,
        profileDefinition: VdsProfileDefinitionDto?,
        tag: Int
    ): MessageCoding {
        // Try extended definition first if available
        if (profileDefinition != null) {
            val definitionMessage = profileDefinition.messages.find { it.tag == tag }
            if (definitionMessage != null) {
                return definitionMessage.coding
            }
        }
        // Fall back to base type
        val docTypeDto = getVdsDocumentTypeDto(baseVdsType)
        return getCoding(docTypeDto, tag.toByte())
    }


    /**
     * Encodes a value into a DER-TLV structure based on the VDS type and message name.
     *
     * The encoding format is determined by the [MessageCoding] defined for the given message
     * in the seal configuration.
     *
     * @param T The type of the input value
     * @param vdsType The VDS type name
     * @param messageName The message name to encode
     * @param inputValue The value to encode
     * @return A [DerTlv] structure containing the encoded value with the appropriate tag
     * @throws IllegalArgumentException if the VDS type or message name is not found
     */
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
        val docTypeDto = getVdsDocumentTypeDto(vdsType)
        return encodeMessage(docTypeDto, messageName, inputValue)
    }

    @Throws(IllegalArgumentException::class)
    private fun <T> encodeMessage(docTypeDto: VdsDocumentTypeDto, messageName: String, inputValue: T): DerTlv {
        val tag = getMessageTag(docTypeDto, messageName)
        if (tag.toInt() == 0) {
            log.w("VdsType: " + docTypeDto.documentType + " has no Message " + messageName)
            throw IllegalArgumentException("VdsType: " + docTypeDto.documentType + " has no Message " + messageName)
        }
        val coding = getCoding(docTypeDto, messageName)
        val value = DataEncoder.encodeValueByCoding(coding, inputValue)
        return DerTlv(tag, value)
    }

    /**
     * Returns the tag number for a given VDS type and message name.
     *
     * @param vdsType The VDS type name
     * @param messageName The message name
     * @return The tag number as integer
     * @throws IllegalArgumentException if the VDS type or message name is not found
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageTag(vdsType: String, messageName: String): Int {
        val docTypeDto = getVdsDocumentTypeDto(vdsType)
        return getMessageTag(docTypeDto, messageName).toInt()
    }

    /**
     * Gets the tag number for a given message name, considering profile definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param profileDefinition The resolved profile definition (can be null)
     * @param messageName The message name to look up
     * @return The tag number
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageTag(baseVdsType: String, profileDefinition: VdsProfileDefinitionDto?, messageName: String): Int {
        if (profileDefinition != null) {
            val definitionMessage = profileDefinition.messages.find { it.name.equals(messageName, ignoreCase = true) }
            if (definitionMessage != null) {
                return definitionMessage.tag
            }
        }
        val docTypeDto = getVdsDocumentTypeDto(baseVdsType)
        return getMessageTag(docTypeDto, messageName).toInt()
    }

    /**
     * Returns the message coding for a given VDS type and tag number.
     *
     * @param vdsType The VDS type name
     * @param tag The tag number
     * @return The [MessageCoding] for the specified tag
     * @throws IllegalArgumentException if the VDS type or tag is not found
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageCoding(vdsType: String, tag: Int): MessageCoding {
        val docTypeDto = getVdsDocumentTypeDto(vdsType)
        return getCoding(docTypeDto, tag.toByte())
    }

    @Throws(IllegalArgumentException::class)
    private fun getMessageTag(docTypeDto: VdsDocumentTypeDto, message: String): Byte {
        for ((name, tag) in docTypeDto.messages) {
            if (name.equals(message, ignoreCase = true)) {
                return tag.toByte()
            }
        }
        throw IllegalArgumentException("Message '" + message + "' is unspecified for the given seal '" + docTypeDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getMessageName(docTypeDto: VdsDocumentTypeDto, tag: Int): String {
        for ((name, tag1) in docTypeDto.messages) {
            if (tag1 == tag) {
                return name
            }
        }
        throw IllegalArgumentException("No Message with tag '" + tag + "' is specified for the given seal '" + docTypeDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(docTypeDto: VdsDocumentTypeDto, message: String): MessageCoding {
        for ((name, _, coding) in docTypeDto.messages) {
            if (name.equals(message, ignoreCase = true)) {
                return coding
            }
        }
        throw IllegalArgumentException("Message '" + message + "' is unspecified for the given seal '" + docTypeDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(docTypeDto: VdsDocumentTypeDto, tag: Byte): MessageCoding {
        for ((_, tag1, coding) in docTypeDto.messages) {
            if (tag1 == tag.toInt()) {
                return coding
            }
        }
        throw IllegalArgumentException("No Message with tag '" + tag + "' is specified for the given seal '" + docTypeDto.documentType + "'")
    }

    /**
     * Encodes a DerTlv to a Message based on the given VDS type.
     *
     * @param vdsType The VDS type name
     * @param derTlv The DerTlv to encode
     * @return The Message, or null if encoding fails
     */
    fun encodeDerTlv(vdsType: String, derTlv: DerTlv): Message? {
        val bytes = derTlv.value
        val name = getMessageName(vdsType, derTlv)
        val tagInt = derTlv.tag.toInt()
        val tagHex = (tagInt and 0xFF).toString(16).uppercase().padStart(2, '0')
        val coding = getMessageCoding(vdsType, derTlv)
        if (name == "" || coding == MessageCoding.UNKNOWN) return null
        return Message(tagHex, name, coding, MessageValue.fromBytes(bytes, coding))
    }

    /**
     * Encodes a DerTlv to a Message with profile definition-aware lookup.
     *
     * @param vdsType The base VDS type
     * @param profileDefinition The resolved profile definition (may be null)
     * @param derTlv The DerTlv to encode
     * @return The Message, or null if encoding fails
     */
    fun encodeDerTlv(vdsType: String, profileDefinition: VdsProfileDefinitionDto?, derTlv: DerTlv): Message? {
        val bytes = derTlv.value
        val tagInt = derTlv.tag.toInt()
        val tagHex = (tagInt and 0xFF).toString(16).uppercase().padStart(2, '0')
        val name = getMessageName(vdsType, profileDefinition, tagInt)
        val coding = getMessageCoding(vdsType, profileDefinition, tagInt)
        if (name == "" || coding == MessageCoding.UNKNOWN) return null
        return Message(tagHex, name, coding, MessageValue.fromBytes(bytes, coding))
    }

    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<VdsDocumentTypeDto>>(jsonString)
        for (dto in newDtos) {
            documentTypeDtoList = documentTypeDtoList.filter { it.documentRef != dto.documentRef } + dto
            if (dto.documentType != "" && dto.documentRef != "") {
                vdsTypes[dto.documentType] = dto.documentRef.toInt(16)
                vdsTypesReverse[dto.documentRef.toInt(16)] = dto.documentType
            }
            dto.messages.forEach { vdsMessages.add(it.name) }
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun getVdsDocumentTypeDto(vdsType: String): VdsDocumentTypeDto {
        for (docTypeDto in documentTypeDtoList) {
            if (docTypeDto.documentType == vdsType) {
                return docTypeDto
            }
        }
        throw IllegalArgumentException("VdsType '$vdsType' is unspecified in VdsDocumentTypes.")
    }


}
