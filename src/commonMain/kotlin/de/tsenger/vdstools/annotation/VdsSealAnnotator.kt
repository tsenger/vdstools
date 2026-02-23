package de.tsenger.vdstools.annotation

import de.tsenger.vdstools.vds.VdsSeal

/**
 * Returns a [SealAnnotation] describing the byte structure of this VDS seal.
 *
 * The annotation covers [VdsSeal.encoded] — i.e., the complete serialized seal bytes.
 * All [ByteRange] offsets are relative to index 0 of that array.
 */
fun VdsSeal.annotate(): SealAnnotation {
    val raw = encoded
    val headerLen = headerBytes.size
    val signedEnd = signedBytes.size  // header + message group

    return SealAnnotation(
        header = buildHeaderAnnotation(raw, headerLen),
        messageGroup = buildMessageGroupAnnotation(raw, headerLen, signedEnd),
        signerCertificate = null,
        signature = if (signedEnd < raw.size)
            FieldAnnotation("Signature (0xFF)", ByteRange(signedEnd, raw.size - signedEnd))
        else null
    )
}

private fun VdsSeal.buildHeaderAnnotation(raw: ByteArray, headerLen: Int): FieldAnnotation {
    var pos = 0
    val version = raw[1].toInt()
    val children = mutableListOf<FieldAnnotation>()

    children += FieldAnnotation("Magic Byte (0xDC)", ByteRange(pos, 1)); pos += 1
    children += FieldAnnotation("Version (0x${raw[1].toHex()})", ByteRange(pos, 1)); pos += 1
    children += FieldAnnotation("Issuing Country ($issuingCountry)", ByteRange(pos, 2)); pos += 2

    // v2 (rawVersion=0x02): signer+certRef always encodes to 6 bytes (9 C40 chars)
    // v3 (rawVersion=0x03): variable length — compute from remaining known-size fields
    val certRefBytes = if (version == 0x02) 6 else headerLen - pos - 8  // 8 = dates(6) + featureRef(1) + typeCat(1)
    children += FieldAnnotation("Signer+CertRef ($signerCertRef)", ByteRange(pos, certRefBytes)); pos += certRefBytes

    children += FieldAnnotation("Issuing Date ($issuingDate)", ByteRange(pos, 3)); pos += 3
    children += FieldAnnotation("Sig Date ($sigDate)", ByteRange(pos, 3)); pos += 3
    children += FieldAnnotation("Doc Feature Ref (0x${docFeatureRef.toHex()})", ByteRange(pos, 1)); pos += 1
    children += FieldAnnotation("Doc Type Cat (0x${docTypeCat.toHex()})", ByteRange(pos, 1))

    return FieldAnnotation("Header", ByteRange(0, headerLen), children)
}

private fun VdsSeal.buildMessageGroupAnnotation(
    raw: ByteArray, headerLen: Int, signedEnd: Int
): FieldAnnotation {
    val msgBytes = raw.copyOfRange(headerLen, signedEnd)
    val spans = scanTlvs(msgBytes, baseOffset = headerLen)

    val children = spans.map { span ->
        val label = messageList.firstOrNull { it.tag == span.tag }?.name
            ?: "Unknown (0x${span.tag.toHex()})"
        FieldAnnotation(
            label, span.range, listOf(
                FieldAnnotation("Tag (0x${span.tag.toHex()})", span.tagRange),
                FieldAnnotation("Length (${span.valueLength} Bytes)", span.lengthRange),
                FieldAnnotation("Value", span.valueRange)
            )
        )
    }

    return FieldAnnotation("Message Group", ByteRange(headerLen, signedEnd - headerLen), children)
}

private fun Byte.toHex() = (toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
private fun Int.toHex() = toString(16).padStart(2, '0').uppercase()
