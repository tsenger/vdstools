package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto

/**
 * TR-03171 presence rules for the message zone, shared by decoding (reported as [FormatFinding]s)
 * and encoding (enforced by the builder).
 */
internal object Tr03171Conformance {

    /**
     * Checks the message zone [tags] of a seal of [baseVdsType] against TR-03171: v0.9 requires
     * PROFILE_URI (0x03) and CERTIFICATE_URI (0x04), and VALID_FROM (0x01) / VALID_TO (0x02) as the
     * profile's validFromPresent / validToPresent demand; all versions require the profile's
     * non-optional entries. The profile number (0x00) is not checked here.
     */
    fun check(baseVdsType: String, tags: Set<Int>, profile: VdsProfileDefinitionDto?): List<FormatFinding> {
        val findings = mutableListOf<FormatFinding>()
        if (baseVdsType == DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9) {
            if (3 !in tags) findings += FormatFinding.MissingRequired(listOf(3), "PROFILE_URI")
            if (4 !in tags) findings += FormatFinding.MissingRequired(listOf(4), "CERTIFICATE_URI")
            if (profile != null) {
                checkPresence(1, "VALID_FROM", profile.validFromPresent, tags, findings)
                checkPresence(2, "VALID_TO", profile.validToPresent, tags, findings)
            }
        }
        profile?.messages?.filter { it.required && it.tag !in tags }
            ?.forEach { findings += FormatFinding.MissingRequired(listOf(it.tag), it.name) }
        return findings
    }

    /** Human-readable text of a finding from [check], for exception messages. */
    fun describe(finding: FormatFinding): String {
        val tag = finding.tagPath.lastOrNull()?.toString(16)?.uppercase()?.padStart(2, '0')
        return when (finding) {
            is FormatFinding.MissingRequired -> "${finding.name} (tag 0x$tag) is required"
            is FormatFinding.NotAllowed -> "${finding.name} (tag 0x$tag) must not be present"
            else -> finding.toString()
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
