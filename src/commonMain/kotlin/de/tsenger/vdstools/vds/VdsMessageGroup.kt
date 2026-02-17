package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import okio.Buffer


class VdsMessageGroup {
    internal var derTlvList: List<DerTlv>
    var vdsType: String
        internal set

    /**
     * The resolved extended message definition for UUID-based seals.
     * Null if no definition lookup was performed or no matching definition was found.
     */
    var extendedMessageDefinition: ExtendedMessageDefinitionDto? = null
        internal set

    /**
     * The document profile UUID (Tag 0) for UUID-based seals.
     * Set during [resolveExtendedMessageDefinition] and excluded from [messageList].
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
                DataEncoder.encodeDerTlv(vdsType, extendedMessageDefinition, derTlv)?.let { messageList.add(it) }
            }
            return messageList
        }

    fun getMessage(messageName: String): Message? {
        return messageList.firstOrNull { message: Message -> message.name == messageName }
    }

    fun getMessage(messageTag: Int): Message? {
        return messageList.firstOrNull { message: Message -> message.tag == messageTag }
    }

    /**
     * Resolves the extended message definition based on the UUID in the specified tag.
     * This method should be called after parsing for seal types that require UUID lookup.
     *
     * @param uuidTag The tag number containing the UUID (typically 0)
     */
    fun resolveExtendedMessageDefinition(uuidTag: Int) {
        val uuidTlv = derTlvList.find { it.tag.toInt() == uuidTag }
        if (uuidTlv != null) {
            documentProfileUuid = uuidTlv.value
            metadataTags.add(uuidTag)
            extendedMessageDefinition = DataEncoder.resolveExtendedMessageDefinition(uuidTlv.value)
        }
    }

    class Builder(val vdsType: String) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)
        internal val baseVdsType: String
        internal val extendedDefinition: ExtendedMessageDefinitionDto?

        init {
            val docRef = DataEncoder.getDocumentRef(vdsType)
            if (docRef == null) {
                // Not a base type — try resolving as extended definition
                val extDef = DataEncoder.resolveExtendedDefinitionByName(vdsType)
                if (extDef != null) {
                    extendedDefinition = extDef
                    baseVdsType = extDef.baseDocumentType
                } else {
                    extendedDefinition = null
                    baseVdsType = vdsType
                }
            } else {
                extendedDefinition = null
                baseVdsType = vdsType
            }
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = DataEncoder.getMessageCoding(baseVdsType, extendedDefinition, tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(name: String, value: T): Builder {
            return addMessage(DataEncoder.getMessageTag(baseVdsType, extendedDefinition, name), value)
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun build(): VdsMessageGroup {
            val group = VdsMessageGroup(this)
            // If using an extended definition, inject UUID as Tag 0 and set definition on group
            if (extendedDefinition != null) {
                val uuidBytes = extendedDefinition.definitionId.hexToByteArray()
                val uuidTlv = DerTlv(0.toByte(), uuidBytes)
                group.derTlvList = listOf(uuidTlv) + group.derTlvList
                group.vdsType = baseVdsType
                group.extendedMessageDefinition = extendedDefinition
                group.documentProfileUuid = uuidBytes
                group.metadataTags.add(0)
            }
            return group
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessageGroup {
            val derTlvList = DataEncoder.parseDerTLvs(rawBytes)
            return VdsMessageGroup(vdsType, derTlvList)
        }
    }
}
