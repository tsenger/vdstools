package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.vds.dto.MessageDto
import okio.Buffer

class IdbMessageGroup {
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
        val hexTag = (messageTag and 0xFF).toString(16).uppercase().padStart(2, '0')
        return messageList.firstOrNull { it.tag == hexTag }
    }

    fun getMessageByTag(messageTag: String): Message? {
        return messageList.firstOrNull { it.tag == messageTag }
    }

    fun getMessageByName(messageName: String): Message? {
        return messageList.firstOrNull { it.name == messageName }
    }

    @Deprecated("Use getMessageByName(messageName) instead", ReplaceWith("getMessageByName(messageName)"))
    fun getMessage(messageName: String): Message? = getMessageByName(messageName)

    @Deprecated("Use getMessageByTag(messageTag) instead", ReplaceWith("getMessageByTag(messageTag)"))
    fun getMessage(messageTag: Int): Message? = getMessageByTag(messageTag)

    val encoded: ByteArray
        get() {
            val messages = Buffer()
            for (derTlv in derTlvList) {
                messages.write(derTlv.encoded)
            }
            return DerTlv(TAG, messages.readByteArray()).encoded
        }

    class Builder(private val subMessageDefs: List<MessageDto>? = null) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        private fun resolveTag(name: String): Int {
            val fromDefs = subMessageDefs?.firstOrNull { it.name == name }?.tag
            if (fromDefs != null) return fromDefs
            return DataEncoder.idbMessageTypes.getMessageType(name) ?: 0
        }

        private fun resolveCoding(name: String, tag: Int): MessageCoding {
            val fromDefs = subMessageDefs?.firstOrNull { it.tag == tag }?.coding
            if (fromDefs != null) return fromDefs
            return DataEncoder.idbMessageTypes.getMessageTypeCoding(name)
        }

        private fun resolveChildDefs(name: String, tag: Int): List<MessageDto>? {
            val fromDefs = subMessageDefs?.firstOrNull { it.tag == tag }?.messages
            if (!fromDefs.isNullOrEmpty()) return fromDefs
            return DataEncoder.idbMessageTypes.getMessageTypeDto(tag)?.messages
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = subMessageDefs?.firstOrNull { it.tag == tag }?.coding
                ?: DataEncoder.idbMessageTypes.getMessageTypeCoding(tag)
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
            val childDefs = subMessageDefs?.firstOrNull { it.tag == tag }?.messages
                ?: DataEncoder.idbMessageTypes.getMessageTypeDto(tag)?.messages
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
            val derTlvList = DataEncoder.parseDerTLvs(valueBytes)
            return IdbMessageGroup(derTlvList)
        }

        private fun parseMessage(derTlv: DerTlv, subMessageDefs: List<MessageDto>?): Message {
            val tagInt = derTlv.tag.toInt() and 0xFF

            val msgDef = subMessageDefs?.firstOrNull { it.tag == tagInt }
            val name = msgDef?.name ?: DataEncoder.idbMessageTypes.getMessageType(tagInt)
            val coding = msgDef?.coding ?: DataEncoder.idbMessageTypes.getMessageTypeCoding(name)
            val value = MessageValue.fromBytes(derTlv.value, coding)

            val childDefs = msgDef?.messages
                ?: DataEncoder.idbMessageTypes.getMessageTypeDto(tagInt)?.messages
            val subMessages = if (!childDefs.isNullOrEmpty() && coding == MessageCoding.BYTES) {
                try {
                    val childTlvs = DataEncoder.parseDerTLvs(derTlv.value)
                    childTlvs.map { childTlv -> parseMessage(childTlv, childDefs) }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val tagHex = tagInt.toString(16).uppercase().padStart(2, '0')
            return Message(tagHex, name, coding, value, subMessages)
        }
    }
}
