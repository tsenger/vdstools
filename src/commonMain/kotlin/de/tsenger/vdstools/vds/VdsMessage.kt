package de.tsenger.vdstools.vds

class VdsMessage(
    val tag: Int,
    val name: String,
    val coding: MessageCoding,
    val value: MessageValue
) {
    override fun toString(): String = "$name: $value"
}
