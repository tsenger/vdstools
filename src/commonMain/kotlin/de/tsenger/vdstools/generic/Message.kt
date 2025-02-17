package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.vds.FeatureCoding

@OptIn(ExperimentalStdlibApi::class)
class Message(typeTag: Int, typeName: String, value: ByteArray, coding: FeatureCoding) {
    val messageTypeTag = typeTag
    val messageTypeName = typeName
    private val messageContent: ByteArray = value
    private val messageCoding = coding

    val valueBytes: ByteArray
        get() = messageContent

    val valueInt: Int
        get() = messageContent[0].toInt() and 0xFF


    val valueStr: String
        get() =
            when (messageCoding) {
                FeatureCoding.BYTE -> valueInt.toString()
                FeatureCoding.C40 -> DataEncoder.decodeC40(messageContent)
                FeatureCoding.UTF8_STRING -> messageContent.decodeToString()
                FeatureCoding.MASKED_DATE -> DataEncoder.decodeMaskedDate(messageContent)
                FeatureCoding.BYTES, FeatureCoding.UNKNOWN -> messageContent.toHexString()
                FeatureCoding.MRZ -> {
                    val unformattedMrz = DataEncoder.decodeC40(messageContent)
                    val mrzLength = when (messageTypeName) {
                        "MRZ_MRVA" -> 88
                        "MRZ_MRVB" -> 72
                        else -> unformattedMrz.length
                    }
                    DataEncoder.formatMRZ(unformattedMrz, mrzLength)
                }
            }
}