package de.tsenger.vdstools.dissect

data class ByteRange(val offset: Int, val length: Int)

data class FieldDissection(
    val label: String,
    val range: ByteRange,
    val children: List<FieldDissection> = emptyList()
)

/**
 * Dissected structure of a parsed seal's raw bytes.
 * Each [FieldDissection] covers a [ByteRange] in the raw byte array that was dissected.
 *
 * For VDS seals: [signerCertificate] is always null.
 * For IDB seals: all fields may be populated.
 */
data class SealDissection(
    val header: FieldDissection,
    val messageGroup: FieldDissection,
    val signerCertificate: FieldDissection?,
    val signature: FieldDissection?
)
