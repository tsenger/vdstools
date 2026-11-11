package de.tsenger.vdstools.generic

import de.tsenger.vdstools.asn1.DerTlv

object MessageResolver {
    fun resolve(derTlv: DerTlv, resolver: MessageDefinitionResolver): Message? {
        val tagHex = (derTlv.tag.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        val definition = resolver.resolveByTag(tagHex) ?: return null
        if (definition.coding == MessageCoding.UNKNOWN) return null
        val value = MessageValue.fromBytes(derTlv.value, definition.coding)
        return Message(derTlv.tag.toInt() and 0xFF, definition.name, definition.coding, value)
    }

    /**
     * A message whose tag has no definition: the raw bytes, not interpreted, named by
     * the tag (e.g. `0x0A`) and marked with coding [MessageCoding.UNKNOWN].
     */
    internal fun unresolved(derTlv: DerTlv): Message {
        val tag = derTlv.tag.toInt() and 0xFF
        val name = "0x" + tag.toString(16).uppercase().padStart(2, '0')
        return Message(tag, name, MessageCoding.UNKNOWN, MessageValue.BytesValue(derTlv.value))
    }
}
