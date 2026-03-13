package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv

object MessageResolver {
    fun resolve(derTlv: DerTlv, resolver: MessageDefinitionResolver): Message? {
        val tagHex = (derTlv.tag.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        val definition = resolver.resolveByTag(tagHex) ?: return null
        if (definition.coding == MessageCoding.UNKNOWN) return null
        val value = MessageValue.fromBytes(derTlv.value, definition.coding)
        return Message(tagHex, definition.name, definition.coding, value)
    }
}
