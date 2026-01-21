package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.MessageCoding
import de.tsenger.vdstools.vds.MessageValue

class IdbMessage(
    val tag: Int,
    val name: String,
    val coding: MessageCoding,
    val value: MessageValue
) {
    val encoded: ByteArray
        get() = DerTlv(tag.toByte(), value.rawBytes).encoded

    override fun toString(): String = "$name: $value"
}
