package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv


class IdbMessage {
    private val messageType: Byte

    val messageContent: ByteArray

    constructor(messageType: IdbMessageType, messageContent: ByteArray) {
        this.messageType = messageType.value
        this.messageContent = messageContent
    }

    constructor(messageType: Byte, messageContent: ByteArray) {
        this.messageType = messageType
        this.messageContent = messageContent
    }

    val encoded: ByteArray
        get() = DerTlv(messageType, messageContent).encoded

    fun getMessageType(): IdbMessageType {
        return IdbMessageType.valueOf(messageType)
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromDerTlv(derTlv: DerTlv): IdbMessage {
            val messageType = IdbMessageType.valueOf(derTlv.tag)
            val messageContent = derTlv.value
            return IdbMessage(messageType, messageContent)
        }

        fun fromByteArray(rawMessageBytes: ByteArray): IdbMessage {
            val tlvMessage = DerTlv.fromByteArray(rawMessageBytes)
            return fromDerTlv(tlvMessage!!)
        }
    }
}
