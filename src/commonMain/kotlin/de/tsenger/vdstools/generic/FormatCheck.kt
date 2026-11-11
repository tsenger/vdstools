package de.tsenger.vdstools.generic

/**
 * Building applies the same rules as reading: throws if [findings] contain
 * [FormatFinding.Category.WRONG_FORMAT] findings, listing all of them.
 */
@Throws(IllegalArgumentException::class)
internal fun requireNoWrongFormat(subject: String, findings: List<FormatFinding>) {
    val violations = findings.filter { it.category == FormatFinding.Category.WRONG_FORMAT }
    require(violations.isEmpty()) {
        "Invalid seal content ($subject): " + violations.joinToString("; ", transform = ::describeViolation)
    }
}

/** Human-readable text of a finding, for the exception message of [requireNoWrongFormat]. */
private fun describeViolation(finding: FormatFinding): String {
    val tag = finding.tagPath.lastOrNull()?.let { " (tag 0x" + it.toString(16).uppercase().padStart(2, '0') + ")" } ?: ""
    return when (finding) {
        is FormatFinding.MissingRequired -> "${finding.name}$tag is required"
        is FormatFinding.NotAllowed -> "${finding.name}$tag must not be present"
        is FormatFinding.DecodeFailed -> "${finding.name}$tag cannot be decoded"
        is FormatFinding.InvalidLength -> "${finding.name}$tag has length ${finding.length}, allowed " +
                "${finding.minLength ?: 0}..${finding.maxLength ?: "unlimited"}"
        else -> finding.toString()
    }
}
