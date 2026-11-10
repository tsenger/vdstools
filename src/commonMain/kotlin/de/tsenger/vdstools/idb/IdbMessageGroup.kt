package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.vds.dto.MessageDto
import okio.Buffer

internal class IdbMessageGroup {
    private var derTlvList: List<DerTlv>

    constructor(derTlvList: List<DerTlv>) {
        this.derTlvList = derTlvList
    }

    private constructor(builder: Builder) {
        this.derTlvList = builder.derTlvList
    }

    val messageList: List<Message>
        get() = derTlvList.map { derTlv ->
            parseMessage(derTlv, null)
        }

    fun getMessageByTag(messageTag: Int): Message? {
        return messageList.firstOrNull { it.tag == messageTag }
    }

    /** Looks up a message by name, ignoring case (names come verbatim from the definition or profile). */
    fun getMessageByName(messageName: String): Message? {
        return messageList.firstOrNull { it.name.equals(messageName, ignoreCase = true) }
    }

    val encoded: ByteArray
        get() {
            val messages = Buffer()
            for (derTlv in derTlvList) {
                messages.write(derTlv.encoded)
            }
            return DerTlv(TAG, messages.readByteArray()).encoded
        }

    /**
     * Builds a message group, or with [subMessageDefs] the content of a container message.
     * Inside a container only the container's own definitions apply; the registry holds
     * top-level message types, whose tags overlap with sub-message tags.
     */
    internal class Builder(private val subMessageDefs: List<MessageDto>? = null) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        private fun resolveTag(name: String): Int {
            if (subMessageDefs != null) {
                return subMessageDefs.firstOrNull { it.name.equals(name, ignoreCase = true) }?.tag
                    ?: throw IllegalArgumentException("Unknown sub-message '$name' for this container")
            }
            return DataEncoder.idbMessageTypes.getMessageType(name) ?: 0
        }

        private fun resolveCoding(name: String, tag: Int): MessageCoding {
            if (subMessageDefs != null) return subMessageDefs.firstOrNull { it.tag == tag }?.coding ?: MessageCoding.UNKNOWN
            return DataEncoder.idbMessageTypes.getMessageTypeCoding(name)
        }

        private fun resolveChildDefs(name: String, tag: Int): List<MessageDto>? {
            if (subMessageDefs != null) return subMessageDefs.firstOrNull { it.tag == tag }?.messages
            return DataEncoder.idbMessageTypes.getMessageTypeDto(tag)?.messages
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = if (subMessageDefs != null) subMessageDefs.firstOrNull { it.tag == tag }?.coding ?: MessageCoding.UNKNOWN
                else DataEncoder.idbMessageTypes.getMessageTypeCoding(tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(name: String, value: T): Builder {
            val tag = resolveTag(name)
            val coding = resolveCoding(name, tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        fun addMessage(name: String, block: Builder.() -> Unit): Builder {
            val tag = resolveTag(name)
            val childDefs = resolveChildDefs(name, tag)
            val childBuilder = Builder(childDefs)
            childBuilder.block()
            val childBytes = Buffer()
            for (childTlv in childBuilder.derTlvList) {
                childBytes.write(childTlv.encoded)
            }
            derTlvList.add(DerTlv(tag.toByte(), childBytes.readByteArray()))
            return this
        }

        fun addMessage(tag: Int, block: Builder.() -> Unit): Builder {
            val childDefs = if (subMessageDefs != null) {
                subMessageDefs.firstOrNull { it.tag == tag }?.messages
                    ?: throw IllegalArgumentException("Unknown sub-message tag $tag for this container")
            } else DataEncoder.idbMessageTypes.getMessageTypeDto(tag)?.messages
            val childBuilder = Builder(childDefs)
            childBuilder.block()
            val childBytes = Buffer()
            for (childTlv in childBuilder.derTlvList) {
                childBytes.write(childTlv.encoded)
            }
            derTlvList.add(DerTlv(tag.toByte(), childBytes.readByteArray()))
            return this
        }

        fun build(): IdbMessageGroup {
            return IdbMessageGroup(this)
        }
    }

    companion object {
        const val TAG: Byte = 0x61

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray): IdbMessageGroup {
            require(rawBytes[0] == TAG) {
                "IdbMessageGroup shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                }, but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            val valueBytes = DerTlv.fromByteArray(rawBytes)?.value ?: ByteArray(0)
            val derTlvList = DerTlv.parseAll(valueBytes)
            return IdbMessageGroup(derTlvList)
        }

        private fun parseMessage(derTlv: DerTlv, subMessageDefs: List<MessageDto>?): Message {
            val tagInt = derTlv.tag.toInt() and 0xFF

            // Inside a container only the container's own definitions apply; the registry
            // holds top-level message types, whose tags overlap with sub-message tags.
            val msgDef = subMessageDefs?.firstOrNull { it.tag == tagInt }
            val isSubMessage = subMessageDefs != null
            val name = msgDef?.name
                ?: if (isSubMessage) "UNKNOWN" else DataEncoder.idbMessageTypes.getMessageType(tagInt)
            val coding = msgDef?.coding
                ?: if (isSubMessage) MessageCoding.UNKNOWN else DataEncoder.idbMessageTypes.getMessageTypeCoding(name)
            val value = MessageValue.fromBytes(derTlv.value, coding)

            val childDefs = msgDef?.messages
                ?: if (isSubMessage) null else DataEncoder.idbMessageTypes.getMessageTypeDto(tagInt)?.messages
            val subMessages = if (!childDefs.isNullOrEmpty() && coding == MessageCoding.SUB_MESSAGES) {
                val expectedTags = childDefs.map { it.tag.toByte() }.toSet()
                val firstByte = derTlv.value.firstOrNull()
                if (firstByte != null && firstByte in expectedTags) {
                    try {
                        val childTlvs = DerTlv.parseAll(derTlv.value)
                        childTlvs.map { childTlv -> parseMessage(childTlv, childDefs) }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            return Message(tagInt, name, coding, value, subMessages)
        }
    }
}
