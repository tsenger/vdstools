package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv
import okio.Buffer

class Message(
    val tag: String,
    val name: String,
    internal val coding: MessageCoding,
    val value: MessageValue,
    val messages: List<Message> = emptyList()
) {
    val encoded: ByteArray
        get() = if (messages.isNotEmpty()) {
            val childBytes = Buffer()
            for (child in messages) {
                childBytes.write(child.encoded)
            }
            DerTlv(tag.toInt(16).toByte(), childBytes.readByteArray()).encoded
        } else {
            DerTlv(tag.toInt(16).toByte(), value.rawBytes).encoded
        }

    fun getMessageByName(name: String): Message? {
        return messages.firstOrNull { it.name == name }
    }

    fun getMessageByTag(tag: Int): Message? {
        val hexTag = (tag and 0xFF).toString(16).uppercase().padStart(2, '0')
        return messages.firstOrNull { it.tag == hexTag }
    }

    fun getMessageByTag(tag: String): Message? {
        return messages.firstOrNull { it.tag == tag }
    }

    override fun toString(): String = value.toString()
}
