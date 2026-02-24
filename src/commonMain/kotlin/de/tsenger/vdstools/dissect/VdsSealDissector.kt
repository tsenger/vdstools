package de.tsenger.vdstools.dissect

import de.tsenger.vdstools.vds.VdsSeal

/**
 * Returns a [SealDissection] describing the byte structure of this VDS seal.
 *
 * The dissection covers [VdsSeal.encoded] — i.e., the complete serialized seal bytes.
 * All [ByteRange] offsets are relative to index 0 of that array.
 */
fun VdsSeal.dissect(): SealDissection {
    val raw = encoded
    val headerLen = headerBytes.size
    val signedEnd = signedBytes.size  // header + message group

    return SealDissection(
        header = buildHeaderDissection(raw, headerLen),
        messageGroup = buildMessageGroupDissection(raw, headerLen, signedEnd),
        signerCertificate = null,
        signature = if (signedEnd < raw.size)
            FieldDissection("Signature (0xFF)", ByteRange(signedEnd, raw.size - signedEnd))
        else null
    )
}

private fun VdsSeal.buildHeaderDissection(raw: ByteArray, headerLen: Int): FieldDissection {
    var pos = 0
    val version = raw[1].toInt()
    val children = mutableListOf<FieldDissection>()

    children += FieldDissection("Magic Byte (0xDC)", ByteRange(pos, 1)); pos += 1
    children += FieldDissection("Version (0x${raw[1].toHex()})", ByteRange(pos, 1)); pos += 1
    children += FieldDissection("Issuing Country ($issuingCountry)", ByteRange(pos, 2)); pos += 2

    // v2 (rawVersion=0x02): signer+certRef always encodes to 6 bytes (9 C40 chars)
    // v3 (rawVersion=0x03): variable length — compute from remaining known-size fields
    val certRefBytes = if (version == 0x02) 6 else headerLen - pos - 8  // 8 = dates(6) + featureRef(1) + typeCat(1)
    children += FieldDissection("Signer+CertRef ($signerCertRef)", ByteRange(pos, certRefBytes)); pos += certRefBytes

    children += FieldDissection("Issuing Date ($issuingDate)", ByteRange(pos, 3)); pos += 3
    children += FieldDissection("Sig Date ($sigDate)", ByteRange(pos, 3)); pos += 3
    children += FieldDissection("Doc Feature Ref (0x${docFeatureRef.toHex()})", ByteRange(pos, 1)); pos += 1
    children += FieldDissection("Doc Type Cat (0x${docTypeCat.toHex()})", ByteRange(pos, 1))

    return FieldDissection("Header", ByteRange(0, headerLen), children)
}

private fun VdsSeal.buildMessageGroupDissection(
    raw: ByteArray, headerLen: Int, signedEnd: Int
): FieldDissection {
    val msgBytes = raw.copyOfRange(headerLen, signedEnd)
    val spans = scanTlvs(msgBytes, baseOffset = headerLen)

    val children = spans.map { span ->
        val label = messageList.firstOrNull { it.tag == span.tag }?.name
            ?: "Unknown (0x${span.tag.toHex()})"
        FieldDissection(
            label, span.range, listOf(
                FieldDissection("Tag (0x${span.tag.toHex()})", span.tagRange),
                FieldDissection("Length (${span.valueLength} Bytes)", span.lengthRange),
                FieldDissection("Value", span.valueRange)
            )
        )
    }

    return FieldDissection("Message Group", ByteRange(headerLen, signedEnd - headerLen), children)
}

private fun Byte.toHex() = (toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
private fun Int.toHex() = toString(16).padStart(2, '0').uppercase()
