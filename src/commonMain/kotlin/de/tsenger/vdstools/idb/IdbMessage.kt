package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding

@OptIn(ExperimentalStdlibApi::class)
class IdbMessage {
    val messageTypeTag: Int
    val messageTypeName: String
    private val messageContent: ByteArray

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

    val valueBytes: ByteArray
        get() = messageContent

    val valueInt: Int
        get() = messageContent[0].toInt() and 0xFF

    val coding: FeatureCoding
        get() = DataEncoder.getIdbMessageTypeCoding(messageTypeName)

    val valueStr: String
        get() =
            when (coding) {
                FeatureCoding.BYTE -> valueInt.toString()
                FeatureCoding.C40 -> DataEncoder.decodeC40(messageContent)
                FeatureCoding.UTF8_STRING -> messageContent.toString()
                FeatureCoding.MASKED_DATE -> DataEncoder.decodeMaskedDate(messageContent)
                FeatureCoding.DATE -> DataEncoder.decodeDate(messageContent).toString()
                FeatureCoding.BYTES, FeatureCoding.UNKNOWN -> messageContent.toHexString()
                FeatureCoding.MRZ -> {
                    val unformattedMrz = DataEncoder.decodeC40(messageContent)
                    DataEncoder.formatMRZ(unformattedMrz, unformattedMrz.length)
                }
            }


    companion object {
        fun fromDerTlv(derTlv: DerTlv): IdbMessage {
            val messageTypeTag = (derTlv.tag).toInt() and 0xFF
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
