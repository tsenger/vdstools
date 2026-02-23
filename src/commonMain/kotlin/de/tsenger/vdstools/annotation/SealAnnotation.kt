package de.tsenger.vdstools.annotation

data class ByteRange(val offset: Int, val length: Int)

data class FieldAnnotation(
    val label: String,
    val range: ByteRange,
    val children: List<FieldAnnotation> = emptyList()
)

/**
 * Annotated structure of a parsed seal's raw bytes.
 * Each [FieldAnnotation] covers a [ByteRange] in the raw byte array that was annotated.
 *
 * For VDS seals: [signerCertificate] is always null.
 * For IDB seals: all fields may be populated.
 */
data class SealAnnotation(
    val header: FieldAnnotation,
    val messageGroup: FieldAnnotation,
    val signerCertificate: FieldAnnotation?,
    val signature: FieldAnnotation?
)
