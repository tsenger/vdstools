package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv
import okio.Buffer

class Message(
    val tag: Int,
    val name: String,
    val coding: MessageCoding,
    val value: MessageValue,
    val messages: List<Message> = emptyList()
) {
    val encoded: ByteArray
        get() = if (messages.isNotEmpty()) {
            val childBytes = Buffer()
            for (child in messages) {
                childBytes.write(child.encoded)
            }
            DerTlv(tag.toByte(), childBytes.readByteArray()).encoded
        } else {
            DerTlv(tag.toByte(), value.rawBytes).encoded
        }

    fun getMessageByName(name: String): Message? {
        return messages.firstOrNull { it.name == name }
    }

    fun getMessageByTag(tag: Int): Message? {
        return messages.firstOrNull { it.tag == tag }
    }

    fun getMessageByTag(tag: String): Message? {
        val tagInt = tag.toIntOrNull(16) ?: return null
        return getMessageByTag(tagInt)
    }

    override fun toString(): String = value.toString()
}
