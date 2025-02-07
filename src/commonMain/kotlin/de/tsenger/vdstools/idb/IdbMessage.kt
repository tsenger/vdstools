package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv


class IdbMessage {
    val messageTypeTag: Int

    val messageTypeName: String

    val messageContent: ByteArray

    constructor(messageTypeName: String, messageContent: ByteArray) {
        this.messageTypeName = messageTypeName
        this.messageTypeTag = DataEncoder.getIdbMessageTypeTag(messageTypeName) ?: 0
        this.messageContent = messageContent
    }

    constructor(messageTypeTag: Int, messageContent: ByteArray) {
        this.messageTypeName = DataEncoder.getIdbMessageTypeName(messageTypeTag)
        this.messageTypeTag = messageTypeTag
        this.messageContent = messageContent
    }

    val encoded: ByteArray
        get() = DerTlv(messageTypeTag.toByte(), messageContent).encoded


    companion object {
        fun fromDerTlv(derTlv: DerTlv): IdbMessage {
            val messageTypeTag = derTlv.tag.toInt()
            val messageContent = derTlv.value
            return IdbMessage(messageTypeTag, messageContent)
        }

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawMessageBytes: ByteArray): IdbMessage {
            val tlvMessage = DerTlv.fromByteArray(rawMessageBytes)
            checkNotNull(tlvMessage)
            return fromDerTlv(tlvMessage)
        }
    }
}
