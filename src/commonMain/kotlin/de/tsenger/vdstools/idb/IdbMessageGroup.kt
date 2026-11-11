package de.tsenger.vdstools.idb

import de.tsenger.vdstools.internal.normalizeCountry
import de.tsenger.vdstools.idb.dto.IdbMessageTypeDto
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageResolver
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.vds.dto.MessageDto
import okio.Buffer

/**
 * The message group of an IDB seal. Its messages are decoded with the IDB message types when the
 * group is created (parsed or built); message types loaded or replaced later do not change it.
 */
internal class IdbMessageGroup(private val derTlvList: List<DerTlv>, issuingCountry: String? = null) {

    /** The message type of each top-level message for the seal's country (null: no definition). */
    private val topLevelDefinitions: List<IdbMessageTypeDto?> =
        derTlvList.map { DataEncoder.idbMessageTypes.getMessageTypeDto(it.tag.toInt() and 0xFF, issuingCountry) }

    val messageList: List<Message> = derTlvList.mapIndexed { i, derTlv -> parseMessage(derTlv, null, topLevelDefinitions[i]) }

    /** Tag and country of each top-level message decoded with a national message type. */
    internal val nationalDefinitions: List<Pair<Int, String>> = derTlvList.indices.mapNotNull { i ->
        topLevelDefinitions[i]?.country?.let { (derTlvList[i].tag.toInt() and 0xFF) to normalizeCountry(it) }
    }

    /** The first top-level message whose type identifies the national document type, with that type. */
    internal val documentTypeIdentifier: Pair<Message, IdbMessageTypeDto>? =
        messageList.indices.firstOrNull { topLevelDefinitions[it]?.identifiesDocumentType == true }
            ?.let { messageList[it] to topLevelDefinitions[it]!! }

    fun getMessageByTag(messageTag: Int): Message? {
        return messageList.firstOrNull { it.tag == messageTag }
    }

    /** Looks up a message by name, ignoring case (names come verbatim from the definition or profile). */
    fun getMessageByName(messageName: String): Message? {
        return messageList.firstOrNull { it.name.equals(messageName, ignoreCase = true) }
    }

    /** Format findings of the messages and sub-messages (see [FormatFinding]). */
    fun formatFindings(): List<FormatFinding> {
        val findings = mutableListOf<FormatFinding>()
        fun walk(messages: List<Message>, parentPath: List<Int>) {
            for (message in messages) {
                val path = parentPath + message.tag
                if (message.isRaw) {
                    findings += if (message.isDefined) FormatFinding.DecodeFailed(path, message.name)
                    else FormatFinding.UnknownTag(path)
                } else if (message.coding == MessageCoding.SUB_MESSAGES && message.messageList.isEmpty()
                    && message.value.rawBytes.isNotEmpty()
                ) {
                    findings += FormatFinding.MalformedSubMessages(path, message.name)
                }
                walk(message.messageList, path)
            }
        }
        walk(messageList, emptyList())
        return findings
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
            return DataEncoder.idbMessageTypes.getMessageType(name)
                ?: throw IllegalArgumentException("Unknown IDB message type '$name'")
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

        fun build(issuingCountry: String? = null): IdbMessageGroup {
            return IdbMessageGroup(derTlvList.toList(), issuingCountry)
        }
    }

    companion object {
        const val TAG: Byte = 0x61

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray, issuingCountry: String? = null): IdbMessageGroup {
            require(rawBytes[0] == TAG) {
                "IdbMessageGroup shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                }, but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            val valueBytes = DerTlv.fromByteArray(rawBytes)?.value ?: ByteArray(0)
            val derTlvList = DerTlv.parseAll(valueBytes)
            return IdbMessageGroup(derTlvList, issuingCountry)
        }

        /** [topLevelDefinition] is the message type of a top-level message; unused inside a container. */
        private fun parseMessage(
            derTlv: DerTlv, subMessageDefs: List<MessageDto>?, topLevelDefinition: IdbMessageTypeDto? = null
        ): Message {
            val tagInt = derTlv.tag.toInt() and 0xFF

            // Inside a container only the container's own definitions apply; the registry
            // holds top-level message types, whose tags overlap with sub-message tags.
            // A tag without definition is returned raw.
            val name: String
            val coding: MessageCoding
            val childDefs: List<MessageDto>
            if (subMessageDefs != null) {
                val msgDef = subMessageDefs.firstOrNull { it.tag == tagInt } ?: return MessageResolver.unresolved(derTlv)
                name = msgDef.name; coding = msgDef.coding; childDefs = msgDef.messages
            } else {
                val dto = topLevelDefinition ?: return MessageResolver.unresolved(derTlv)
                name = dto.name; coding = dto.coding; childDefs = dto.messages
            }
            val value = MessageValue.fromBytes(derTlv.value, coding)

            // Content that is no valid DER-TLV sequence yields no sub-messages (reported as
            // FormatFinding.MalformedSubMessages); an unknown sub-tag is returned raw.
            val subMessages = if (childDefs.isNotEmpty() && coding == MessageCoding.SUB_MESSAGES) {
                try {
                    DerTlv.parseAll(derTlv.value).map { childTlv -> parseMessage(childTlv, childDefs) }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            return Message(tagInt, name, coding, value, subMessages)
        }
    }
}
