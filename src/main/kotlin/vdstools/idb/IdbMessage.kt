package vdstools.idb

import vdstools.asn1.DerTlv


class IdbMessage {
    private val messageType: Byte

    @JvmField
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

    fun getMessageType(): IdbMessageType? {
        return IdbMessageType.valueOf(messageType)
    }

    companion object {
        @JvmStatic
        fun fromDerTlv(derTlv: DerTlv): IdbMessage {
            val messageType = IdbMessageType.valueOf(derTlv.tag)
            assert(messageType != null)
            val messageContent = derTlv.value
            return IdbMessage(messageType!!, messageContent)
        }

        @JvmStatic
        fun fromByteArray(rawMessageBytes: ByteArray): IdbMessage {
            val tlvMessage = DerTlv.fromByteArray(rawMessageBytes)
            return fromDerTlv(tlvMessage!!)
        }
    }
}
