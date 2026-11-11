package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto

/**
 * TR-03171 rules for the message zone that depend on the profile, reported as [FormatFinding]s.
 * The builder enforces them through the same findings.
 */
internal object Tr03171Conformance {

    /**
     * Checks the message zone [tags] of a seal of [baseVdsType] against the rules that depend on its
     * TR-03171 [profile]: v0.9 VALID_FROM (0x01) / VALID_TO (0x02) as the profile's validFromPresent /
     * validToPresent demand, and the profile's non-optional entries for all versions. The rules of the
     * base type itself (e.g. the mandatory PROFILE_URI and CERTIFICATE_URI of v0.9) are data: they are
     * checked via its definition in `VdsDocumentTypes.json` (see `VdsDocumentTypeDto.specification`).
     */
    fun check(baseVdsType: String, tags: Set<Int>, profile: VdsProfileDefinitionDto?): List<FormatFinding> {
        val findings = mutableListOf<FormatFinding>()
        if (baseVdsType == DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9 && profile != null) {
            checkPresence(1, "VALID_FROM", profile.validFromPresent, tags, findings)
            checkPresence(2, "VALID_TO", profile.validToPresent, tags, findings)
        }
        profile?.messages?.filter { it.required && it.tag !in tags }
            ?.forEach { findings += FormatFinding.MissingRequired(listOf(it.tag), it.name) }
        return findings
    }

    /**
     * Checks the decoded [messages] against the explicit maximum lengths of the [profile]'s entries
     * (TR-03171 §4): characters for UTF8String, bytes for OCTET STRING. Raw messages are skipped.
     */
    fun checkLengths(messages: List<Message>, profile: VdsProfileDefinitionDto): List<FormatFinding> {
        val maxLengths = profile.messages.filter { it.maxLength != null }.associate { it.tag to it.maxLength!! }
        return messages.filter { !it.isRaw && it.tag in maxLengths }.mapNotNull { message ->
            val maxLength = maxLengths.getValue(message.tag)
            val length = if (message.coding == MessageCoding.UTF8_STRING) {
                message.value.toString().let { text -> text.length - text.count { it.isLowSurrogate() } }
            } else {
                message.value.rawBytes.size
            }
            if (length > maxLength) FormatFinding.InvalidLength(listOf(message.tag), message.name, length, null, maxLength)
            else null
        }
    }

    private fun checkPresence(
        tag: Int, name: String, mustBePresent: Boolean?, tags: Set<Int>, findings: MutableList<FormatFinding>
    ) {
        if (mustBePresent == null || (tag in tags) == mustBePresent) return
        findings += if (mustBePresent) FormatFinding.MissingRequired(listOf(tag), name)
        else FormatFinding.NotAllowed(listOf(tag), name)
    }
}
