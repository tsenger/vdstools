package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv
import okio.Buffer

class Message(
    val tag: Int,
    val name: String,
    internal val coding: MessageCoding,
    val value: MessageValue,
    val messageList: List<Message> = emptyList()
) {
    val encoded: ByteArray
        get() = if (messageList.isNotEmpty()) {
            val childBytes = Buffer()
            for (child in messageList) {
                childBytes.write(child.encoded)
            }
            DerTlv(tag.toByte(), childBytes.readByteArray()).encoded
        } else {
            DerTlv(tag.toByte(), value.rawBytes).encoded
        }

    /** Looks up a sub-message by name, ignoring case. */
    fun getMessageByName(name: String): Message? {
        return messageList.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun getMessageByTag(tag: Int): Message? {
        return messageList.firstOrNull { it.tag == tag }
    }

    override fun toString(): String = value.toString()
}
