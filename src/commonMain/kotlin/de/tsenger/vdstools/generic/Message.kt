package de.tsenger.vdstools.generic

import de.tsenger.vdstools.vds.MessageCoding
import de.tsenger.vdstools.vds.MessageValue

class Message(
    val messageTypeTag: Int,
    val messageTypeName: String,
    val coding: MessageCoding,
    val value: MessageValue
) {
    override fun toString(): String = "$messageTypeName: $value"
}
