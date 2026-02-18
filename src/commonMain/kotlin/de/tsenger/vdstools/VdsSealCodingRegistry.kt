package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import de.tsenger.vdstools.vds.dto.SealDto
import kotlinx.serialization.json.Json


/**
 * Central component for processing VDS (Visible Digital Seal) message definitions.
 *
 * This class loads seal definitions from a JSON configuration and provides functionality for:
 * - Bidirectional mapping between VDS type names and their hexadecimal document references
 * - Encoding values into DER-TLV format based on type and message specifications
 * - Decoding messages by resolving tag numbers to message names and codings
 * - Supporting extended message definitions with fallback to base types
 * - UUID-based profile lookup for certain VDS types
 *
 * @param jsonString JSON string containing an array of [SealDto] definitions
 */
class VdsSealCodingRegistry(jsonString: String) {
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
        val sealDto = getSealDto(vdsType)
        return getMessageName(sealDto, derTlv.tag.toInt())
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
        val sealDto = getSealDto(vdsType)
        return getMessageTag(sealDto, messageName).toInt()
    }

    /**
     * Gets the tag number for a given message name, considering extended message definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended message definition (can be null)
     * @param messageName The message name to look up
     * @return The tag number
     */
    @Throws(IllegalArgumentException::class)
    fun getMessageTag(baseVdsType: String, extendedDefinition: ExtendedMessageDefinitionDto?, messageName: String): Int {
        if (extendedDefinition != null) {
            val definitionMessage = extendedDefinition.messages.find { it.name.equals(messageName, ignoreCase = true) }
            if (definitionMessage != null) {
                return definitionMessage.tag
            }
        }
        val sealDto = getSealDto(baseVdsType)
        return getMessageTag(sealDto, messageName).toInt()
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

    /**
     * Finds a compound message definition by name.
     * A compound message has [MessageDto.compoundTag] set (non-null).
     *
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended message definition (can be null)
     * @param messageName The message name to look up (e.g., "VALID_FROM")
     * @return The matching [MessageDto] if it is a compound message, or null
     */
    fun findCompoundMessage(
        baseVdsType: String,
        extendedDefinition: ExtendedMessageDefinitionDto?,
        messageName: String
    ): de.tsenger.vdstools.vds.dto.MessageDto? {
        if (extendedDefinition != null) {
            val msg = extendedDefinition.messages.find {
                it.name.equals(messageName, ignoreCase = true) && it.compoundTag != null
            }
            if (msg != null) return msg
        }
        val sealDto = try { getSealDto(baseVdsType) } catch (_: IllegalArgumentException) { return null }
        return sealDto.messages.find {
            it.name.equals(messageName, ignoreCase = true) && it.compoundTag != null
        }
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
