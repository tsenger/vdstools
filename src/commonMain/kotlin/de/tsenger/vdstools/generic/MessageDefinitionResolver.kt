package de.tsenger.vdstools.generic

interface MessageDefinitionResolver {
    fun resolveByTag(tag: String): MessageDefinition?
    fun resolveByName(name: String): MessageDefinition?
}
