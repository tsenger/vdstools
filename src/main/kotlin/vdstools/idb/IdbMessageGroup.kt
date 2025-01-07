package vdstools.idb

import vdstools.DataParser
import vdstools.asn1.DerTlv
import java.io.ByteArrayOutputStream
import java.io.IOException

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

    @get:Throws(IOException::class)
    val encoded: ByteArray
        get() {
            val messages = ByteArrayOutputStream()
            for (message in messagesList) {
                messages.write(message.encoded)
            }
            return DerTlv(TAG, messages.toByteArray()).encoded
        }

    companion object {
        const val TAG: Byte = 0x61

        @JvmStatic
        @Throws(IOException::class)
        fun fromByteArray(rawBytes: ByteArray): IdbMessageGroup {
            require(rawBytes[0] == TAG) {
                String.format(
                    "IdbMessageGroup shall have tag %2X, but tag %2X was found instead.", TAG,
                    rawBytes[0]
                )
            }
            val valueBytes = DerTlv.fromByteArray(rawBytes)?.value ?: ByteArray(0)
            val messageGroup = IdbMessageGroup()
            val derTlvMessagesList = DataParser.parseDerTLvs(valueBytes)
            for (derTlvMessage in derTlvMessagesList) {
                messageGroup.addMessage(IdbMessage.fromDerTlv(derTlvMessage))
            }
            return messageGroup
        }
    }
}
