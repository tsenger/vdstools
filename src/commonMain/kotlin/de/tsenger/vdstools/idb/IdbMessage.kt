package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.FeatureValue

class IdbMessage(
    val messageTypeTag: Int,
    val messageTypeName: String,
    val coding: FeatureCoding,
    val value: FeatureValue
) {
    val encoded: ByteArray
        get() = DerTlv(messageTypeTag.toByte(), value.rawBytes).encoded

    override fun toString(): String = "$messageTypeName: $value"

    companion object {
        fun fromDerTlv(derTlv: DerTlv): IdbMessage {
            val tag = derTlv.tag.toInt() and 0xFF
            val name = DataEncoder.getIdbMessageTypeName(tag)
            val coding = DataEncoder.getIdbMessageTypeCoding(name)
            val value = FeatureValue.fromBytes(derTlv.value, coding)
            return IdbMessage(tag, name, coding, value)
        }

        fun fromByteArray(rawMessageBytes: ByteArray): IdbMessage {
            val tlvMessage = DerTlv.fromByteArray(rawMessageBytes)
            checkNotNull(tlvMessage)
            return fromDerTlv(tlvMessage)
        }

        fun fromNameAndContent(name: String, content: ByteArray): IdbMessage {
            val tag = DataEncoder.getIdbMessageTypeTag(name) ?: 0
            val coding = DataEncoder.getIdbMessageTypeCoding(name)
            val value = FeatureValue.fromBytes(content, coding)
            return IdbMessage(tag, name, coding, value)
        }

        fun fromTagAndContent(tag: Int, content: ByteArray): IdbMessage {
            val name = DataEncoder.getIdbMessageTypeName(tag)
            val coding = DataEncoder.getIdbMessageTypeCoding(name)
            val value = FeatureValue.fromBytes(content, coding)
            return IdbMessage(tag, name, coding, value)
        }

        /**
         * Creates an IdbMessage with raw bytes that are not decoded.
         * Useful for testing or when the bytes should be stored as-is.
         */
        fun fromNameAndRawBytes(name: String, rawBytes: ByteArray): IdbMessage {
            val tag = DataEncoder.getIdbMessageTypeTag(name) ?: 0
            val coding = DataEncoder.getIdbMessageTypeCoding(name)
            val value = FeatureValue.BytesValue(rawBytes)
            return IdbMessage(tag, name, coding, value)
        }
    }
}
