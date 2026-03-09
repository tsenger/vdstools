package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv

class Message(
    val tag: String,
    val name: String,
    val coding: MessageCoding,
    val value: MessageValue
) {
    val encoded: ByteArray
        get() = DerTlv(tag.toInt(16).toByte(), value.rawBytes).encoded

    override fun toString(): String = value.toString()
}
