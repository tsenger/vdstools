package de.tsenger.vdstools.dissect

import de.tsenger.vdstools.idb.IdbSeal

/**
 * Returns a [SealDissection] describing the byte structure of this IDB seal's payload.
 *
 * The dissection covers the decoded payload bytes — i.e., the Base32-decoded
 * (and optionally unzipped) bytes, NOT the raw barcode string.
 * All [ByteRange] offsets are relative to index 0 of those payload bytes.
 *
 * To display the hex view in a UI, use the same component bytes in this order:
 *   payLoad.idbHeader.encoded + payLoad.idbMessageGroup.encoded
 *   + (payLoad.idbSignerCertificate?.encoded ?: byteArrayOf())
 *   + (payLoad.idbSignature?.encoded ?: byteArrayOf())
 */
fun IdbSeal.dissect(): SealDissection {
    val headerBytes = payLoad.idbHeader.encoded
    val msgGroupBytes = payLoad.idbMessageGroup.encoded
    val certBytes = payLoad.idbSignerCertificate?.encoded ?: byteArrayOf()
    val sigBytes = payLoad.idbSignature?.encoded ?: byteArrayOf()

    val headerLen = headerBytes.size
    val msgGroupStart = headerLen
    val certStart = msgGroupStart + msgGroupBytes.size
    val sigStart = certStart + certBytes.size

    return SealDissection(
        header = buildIdbHeaderDissection(),
        messageGroup = buildIdbMessageGroupDissection(msgGroupBytes, msgGroupStart),
        signerCertificate = if (certBytes.isNotEmpty())
            FieldDissection("Signer Certificate (0x7E)", ByteRange(certStart, certBytes.size))
        else null,
        signature = if (sigBytes.isNotEmpty())
            FieldDissection("Signature (0x7F)", ByteRange(sigStart, sigBytes.size))
        else null
    )
}

private fun IdbSeal.buildIdbHeaderDissection(): FieldDissection {
    val header = payLoad.idbHeader
    val headerLen = header.encoded.size
    var pos = 0
    val children = mutableListOf<FieldDissection>()

    children += FieldDissection("Country Identifier (${header.getCountryIdentifier()})", ByteRange(pos, 2)); pos += 2
    if (headerLen == 12) {
        children += FieldDissection(
            "Signature Algorithm (${header.getSignatureAlgorithm()?.name})",
            ByteRange(pos, 1)
        ); pos += 1
        children += FieldDissection(
            "Certificate Reference (${header.certificateReference?.toHex()})",
            ByteRange(pos, 5)
        ); pos += 5
        children += FieldDissection("Signature Creation Date (${header.getSignatureCreationDate()})", ByteRange(pos, 4))
    }

    return FieldDissection("Header", ByteRange(0, headerLen), children)
}

private fun IdbSeal.buildIdbMessageGroupDissection(
    msgGroupBytes: ByteArray, baseOffset: Int
): FieldDissection {
    // msgGroupBytes = [0x61][DER length field][inner TLV1][inner TLV2]...
    // Scan the outer 0x61 TLV to find where the inner content starts.
    val outerSpans = scanTlvs(msgGroupBytes, baseOffset = 0)
    val outerSpan = outerSpans.firstOrNull()
        ?: return FieldDissection("Message Group (0x61)", ByteRange(baseOffset, msgGroupBytes.size))

    // Scan inner TLVs using the absolute offset of the first inner byte.
    val innerAbsoluteOffset = baseOffset + outerSpan.valueOffset
    val innerBytes = msgGroupBytes.copyOfRange(outerSpan.valueOffset, msgGroupBytes.size)
    val innerSpans = scanTlvs(innerBytes, baseOffset = innerAbsoluteOffset)

    val children = innerSpans.map { inner ->
        val label = payLoad.idbMessageGroup.messageList
            .firstOrNull { it.tag == inner.tag }?.name
            ?: "Unknown (0x${inner.tag.toHex()})"
        FieldDissection(
            label, inner.range, listOf(
                FieldDissection("Tag (0x${inner.tag.toHex()})", inner.tagRange),
                FieldDissection("Length (${inner.valueLength} Bytes)", inner.lengthRange),
                FieldDissection("Value", inner.valueRange)
            )
        )
    }

    return FieldDissection("Message Group (0x61)", ByteRange(baseOffset, msgGroupBytes.size), children)
}

private fun Byte.toHex() = (toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
private fun Int.toHex() = toString(16).padStart(2, '0').uppercase()
private fun ByteArray.toHex() = joinToString("") { it.toHex() }
