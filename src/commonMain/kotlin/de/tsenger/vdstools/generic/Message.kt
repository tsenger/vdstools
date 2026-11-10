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
    /**
     * True if [value] holds the uninterpreted raw bytes of this message instead of a decoded value:
     * either no definition exists for its tag (the name is then the tag in hex, e.g. `0x0A`), or
     * the bytes cannot be decoded with the coding of its definition. A message defined as plain
     * bytes, and a container message (whose interpretation are its sub-messages), is not raw.
     */
    val isRaw: Boolean
        get() = coding == MessageCoding.UNKNOWN || (value is MessageValue.BytesValue
                && coding != MessageCoding.BYTES && coding != MessageCoding.SUB_MESSAGES)

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
