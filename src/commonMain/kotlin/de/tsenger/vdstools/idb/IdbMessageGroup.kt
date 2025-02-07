package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import okio.Buffer

class IdbMessageGroup {
    private val messagesList: MutableList<IdbMessage> = ArrayList()

    constructor()

    constructor(idbMessage: IdbMessage) {
        addMessage(idbMessage)
    }

    fun addMessage(idbMessage: IdbMessage) {
        messagesList.add(idbMessage)
    }

    fun getMessagesList(): List<IdbMessage> {
        return messagesList
    }

    val encoded: ByteArray
        get() {
            val messages = Buffer()
            for (message in messagesList) {
                messages.write(message.encoded)
            }
            return DerTlv(TAG, messages.readByteArray()).encoded
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
            val messageGroup = IdbMessageGroup()
            val derTlvMessagesList = DataEncoder.parseDerTLvs(valueBytes)
            for (derTlvMessage in derTlvMessagesList) {
                messageGroup.addMessage(IdbMessage.fromDerTlv(derTlvMessage))
            }
            return messageGroup
        }
    }
}
