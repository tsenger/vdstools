package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
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

    @Throws(IllegalArgumentException::class)
    inline fun <reified T> addMessage(tag: Int, value: T) {
        val coding = DataEncoder.getIdbMessageTypeCoding(tag)
        when (value) {
            is String, is ByteArray, is Int -> {
                when (coding) {
                    FeatureCoding.C40 -> addMessage(IdbMessage(tag, DataEncoder.encodeC40(value as String)))
                    FeatureCoding.UTF8_STRING -> addMessage(IdbMessage(tag, (value as String).encodeToByteArray()))
                    FeatureCoding.BYTES -> addMessage(IdbMessage(tag, value as ByteArray))
                    FeatureCoding.BYTE -> addMessage(IdbMessage(tag, byteArrayOf(((value as Int) and 0xFF).toByte())))
                    FeatureCoding.MASKED_DATE -> addMessage(
                        IdbMessage(
                            tag,
                            DataEncoder.encodeMaskedDate(value as String)
                        )
                    )

                    FeatureCoding.MRZ -> addMessage(IdbMessage(tag, DataEncoder.encodeC40(value as String)))
                    FeatureCoding.UNKNOWN -> throw IllegalArgumentException("Unsupported tag: $tag")
                }
            }

            else -> throw IllegalArgumentException("Unsupported type: ${T::class.simpleName}")
        }
    }

    fun getMessagesList(): List<IdbMessage> {
        return messagesList
    }

    fun getMessage(messageTag: Int): IdbMessage? {
        return messagesList.firstOrNull { it.messageTypeTag == messageTag }
    }

    fun getMessage(messageName: String): IdbMessage? {
        return messagesList.firstOrNull { it.messageTypeName == messageName }
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
