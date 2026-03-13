package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import okio.Buffer


class VdsMessageGroup {
    internal var derTlvList: List<DerTlv>
    var vdsType: String
        internal set

    /**
     * The resolved extended message definition for UUID-based seals.
     * Null if no definition lookup was performed or no matching definition was found.
     */
    var profileDefinition: VdsProfileDefinitionDto? = null
        internal set

    /**
     * The document profile UUID (Tag 0) for UUID-based seals.
     * Set during [resolveProfileDefinition] and excluded from [messageList].
     */
    var documentProfileUuid: ByteArray? = null
        internal set

    internal val metadataTags: MutableSet<Int> = mutableSetOf()

    constructor(vdsType: String, derTlvList: List<DerTlv>) {
        this.vdsType = vdsType
        this.derTlvList = derTlvList
    }

    private constructor(builder: Builder) {
        this.derTlvList = builder.derTlvList
        this.vdsType = builder.vdsType
    }

    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            for (derTlv in derTlvList) {
                buffer.write(derTlv.encoded)
            }
            return buffer.readByteArray()
        }

    val messageList: List<Message>
        /**
         * @return a list of all decoded Messages, using extended message definition-aware lookup if available.
         * Metadata tags (e.g. UUID tag) are excluded.
         */
        get() {
            val messageList: MutableList<Message> = ArrayList()
            for (derTlv in derTlvList) {
                if (derTlv.tag.toInt() in metadataTags) continue
                DataEncoder.vdsDocumentTypes.resolveMessage(vdsType, profileDefinition, derTlv)
                    ?.let { messageList.add(it) }
            }
            return messageList
        }

    val metadataMessageList: List<Message>
        get() {
            val result: MutableList<Message> = ArrayList()
            for (derTlv in derTlvList) {
                if (derTlv.tag.toInt() !in metadataTags) continue
                DataEncoder.vdsDocumentTypes.resolveMessage(vdsType, profileDefinition, derTlv)
                    ?.let { result.add(it) }
            }
            return result
        }

    fun getMessageByName(messageName: String): Message? {
        return messageList.firstOrNull { message: Message -> message.name == messageName }
    }

    fun getMessageByTag(messageTag: Int): Message? {
        val hexTag = (messageTag and 0xFF).toString(16).uppercase().padStart(2, '0')
        return messageList.firstOrNull { message: Message -> message.tag == hexTag }
    }

    fun getMessageByTag(messageTag: String): Message? {
        return messageList.firstOrNull { message: Message -> message.tag == messageTag }
    }

    @Deprecated("Use getMessageByName(messageName) instead", ReplaceWith("getMessageByName(messageName)"))
    fun getMessage(messageName: String): Message? = getMessageByName(messageName)

    @Deprecated("Use getMessageByTag(messageTag) instead", ReplaceWith("getMessageByTag(messageTag)"))
    fun getMessage(messageTag: Int): Message? = getMessageByTag(messageTag)

    /**
     * Resolves the extended message definition based on the UUID in the specified tag.
     * This method should be called after parsing for seal types that require UUID lookup.
     *
     * @param uuidTag The tag number containing the UUID (typically 0)
     */
    fun resolveProfileDefinition(uuidTag: Int) {
        val uuidTlv = derTlvList.find { it.tag.toInt() == uuidTag }
        if (uuidTlv != null) {
            documentProfileUuid = uuidTlv.value
            metadataTags.add(uuidTag)
            profileDefinition = DataEncoder.vdsProfileDefinitions.resolve(uuidTlv.value)
        }
    }

    class Builder(val vdsType: String) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)
        internal val baseVdsType: String
        internal val profileDefinition: VdsProfileDefinitionDto?

        init {
            val docRef = DataEncoder.vdsDocumentTypes.getDocumentRef(vdsType)
            if (docRef == null) {
                // Not a base type — try resolving as extended definition
                val extDef = DataEncoder.vdsProfileDefinitions.resolveByName(vdsType)
                if (extDef != null) {
                    profileDefinition = extDef
                    baseVdsType = extDef.baseDocumentType
                } else {
                    profileDefinition = null
                    baseVdsType = vdsType
                }
            } else {
                profileDefinition = null
                baseVdsType = vdsType
            }
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = DataEncoder.vdsDocumentTypes.getMessageCoding(baseVdsType, profileDefinition, tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(name: String, value: T): Builder {
            return addMessage(DataEncoder.vdsDocumentTypes.getMessageTag(baseVdsType, profileDefinition, name), value)
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun build(): VdsMessageGroup {
            val group = VdsMessageGroup(this)
            // If using an extended definition, inject UUID as Tag 0 and set definition on group
            if (profileDefinition != null) {
                val uuidBytes = profileDefinition.definitionId.hexToByteArray()
                val uuidTlv = DerTlv(0.toByte(), uuidBytes)
                group.derTlvList = listOf(uuidTlv) + group.derTlvList
                group.vdsType = baseVdsType
                group.profileDefinition = profileDefinition
                group.documentProfileUuid = uuidBytes
                DataEncoder.vdsDocumentTypes.getMetadataTags(baseVdsType).forEach { group.metadataTags.add(it) }
                if (group.metadataTags.isEmpty()) group.metadataTags.add(0)
            }
            return group
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessageGroup {
            val derTlvList = DerTlv.parseAll(rawBytes)
            return VdsMessageGroup(vdsType, derTlvList)
        }
    }
}
