package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import okio.Buffer


class VdsMessageGroup {
    private var derTlvList: List<DerTlv>
    var vdsType: String
        private set

    /**
     * The resolved extended message definition for UUID-based seals.
     * Null if no definition lookup was performed or no matching definition was found.
     */
    var extendedMessageDefinition: ExtendedMessageDefinitionDto? = null
        private set

    /**
     * The effective VDS type, considering extended message definition resolution.
     * Returns the definition name if resolved, otherwise the base vdsType.
     */
    val effectiveVdsType: String
        get() = extendedMessageDefinition?.definitionName ?: vdsType

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
         * @return a list of all decoded Messages, using extended message definition-aware lookup if available
         */
        get() {
            val messageList: MutableList<Message> = ArrayList()
            for (derTlv in derTlvList) {
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
     * @return The definition name if resolved, or the base vdsType if no definition found
     */
    fun resolveExtendedMessageDefinition(uuidTag: Int): String {
        val uuidTlv = derTlvList.find { it.tag.toInt() == uuidTag }
        if (uuidTlv != null) {
            extendedMessageDefinition = DataEncoder.resolveExtendedMessageDefinition(uuidTlv.value)
        }
        return effectiveVdsType
    }

    class Builder(val vdsType: String) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = DataEncoder.getMessageCoding(vdsType, tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(name: String, value: T): Builder {
            return addMessage(DataEncoder.getMessageTag(vdsType, name), value)
        }

        fun build(): VdsMessageGroup {
            return VdsMessageGroup(this)
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessageGroup {
            val derTlvList = DataEncoder.parseDerTLvs(rawBytes)
            return VdsMessageGroup(vdsType, derTlvList)
        }
    }
}
