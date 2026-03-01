package de.tsenger.vdstools.dissect

/**
 * Represents a single DER-encoded TLV (Tag-Length-Value) element,
 * tracking its position and structure within a byte array.
 *
 * All offset values are absolute (relative to the start of the outermost byte array).
 */
internal data class TlvSpan(
    val tag: Int,
    val tagOffset: Int,
    val lengthFieldSize: Int,
    val valueLength: Int
) {
    val totalLength: Int    get() = 1 + lengthFieldSize + valueLength
    val valueOffset: Int    get() = tagOffset + 1 + lengthFieldSize
    val range: ByteRange        get() = ByteRange(tagOffset, totalLength)
    val tagRange: ByteRange     get() = ByteRange(tagOffset, 1)
    val lengthRange: ByteRange  get() = ByteRange(tagOffset + 1, lengthFieldSize)
    val valueRange: ByteRange   get() = ByteRange(valueOffset, valueLength)
}

/**
 * Scans [bytes] for DER-encoded TLV structures and returns a [TlvSpan] for each.
 *
 * @param bytes The bytes to scan.
 * @param baseOffset Absolute offset of [bytes[0]] in the outermost byte array.
 *                   All returned [TlvSpan.tagOffset] values are relative to this base.
 */
internal fun scanTlvs(bytes: ByteArray, baseOffset: Int): List<TlvSpan> {
    var pos = 0
    val result = mutableListOf<TlvSpan>()
    while (pos < bytes.size) {
        val tagOffset = baseOffset + pos
        val tag = bytes[pos].toInt() and 0xFF
        pos += 1

        val firstLenByte = bytes[pos].toInt() and 0xFF
        pos += 1
        val derLen = decodeDerLength(firstLenByte, bytes, pos)
        pos += derLen.extraBytes

        result.add(TlvSpan(tag, tagOffset, 1 + derLen.extraBytes, derLen.value))
        pos += derLen.value
    }
    return result
}

private data class DerLength(val value: Int, val extraBytes: Int)

private fun decodeDerLength(firstByte: Int, bytes: ByteArray, pos: Int): DerLength {
    return when (firstByte) {
        0x81 -> DerLength(
            bytes[pos].toInt() and 0xFF,
            1
        )
        0x82 -> DerLength(
            ((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF),
            2
        )
        0x83 -> DerLength(
            ((bytes[pos].toInt() and 0xFF) shl 16) or
                    ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                    (bytes[pos + 2].toInt() and 0xFF),
            3
        )
        else -> DerLength(firstByte, 0)  // <= 0x7F: length is the byte itself
    }
}
