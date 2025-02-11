package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
import okio.Buffer

class IdbMessageGroup {
    var messagesList: List<IdbMessage> = emptyList()
        private set


    constructor(messagesList: List<IdbMessage>) {
        this.messagesList = messagesList
    }

    private constructor(builder: Builder) {
        this.messagesList = builder.messageList
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

    class Builder() {
        val messageList: MutableList<IdbMessage> = ArrayList(5)

        @Throws(IllegalArgumentException::class)
        inline fun <reified T> addMessage(tag: Int, value: T): Builder {
            val coding = DataEncoder.getIdbMessageTypeCoding(tag)
            when (value) {
                is String, is ByteArray, is Int -> {
                    when (coding) {
                        FeatureCoding.C40 -> messageList.add(IdbMessage(tag, DataEncoder.encodeC40(value as String)))
                        FeatureCoding.UTF8_STRING -> messageList.add(
                            IdbMessage(
                                tag,
                                (value as String).encodeToByteArray()
                            )
                        )

                        FeatureCoding.BYTES -> messageList.add(IdbMessage(tag, value as ByteArray))
                        FeatureCoding.BYTE -> messageList.add(
                            IdbMessage(
                                tag,
                                byteArrayOf(((value as Int) and 0xFF).toByte())
                            )
                        )

                        FeatureCoding.MASKED_DATE -> messageList.add(
                            IdbMessage(
                                tag,
                                DataEncoder.encodeMaskedDate(value as String)
                            )
                        )

                        FeatureCoding.MRZ -> messageList.add(IdbMessage(tag, DataEncoder.encodeC40(value as String)))
                        FeatureCoding.UNKNOWN -> throw IllegalArgumentException("Unsupported tag: $tag")
                    }
                }

                else -> throw IllegalArgumentException("Unsupported type: ${T::class.simpleName}")
            }
            return this
        }

        @Throws(IllegalArgumentException::class)
        inline fun <reified T> addMessage(name: String, value: T): Builder {
            return addMessage(DataEncoder.getIdbMessageTypeTag(name) ?: 0, value)
        }

        fun build(): IdbMessageGroup {
            return IdbMessageGroup(this)
        }
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
            val derTlvMessagesList = DataEncoder.parseDerTLvs(valueBytes)

            val messageList = derTlvMessagesList.map {
                IdbMessage.fromDerTlv(it)
            }
            return IdbMessageGroup(messageList)
        }
    }
}
