package de.tsenger.vdstools.asn1


class DerTlv(val tag: Byte, val value: ByteArray) {

    val encoded: ByteArray
        /**
         * wraps the given data (Value) in a DER TLV object with free choice of the tag
         * Length will be calculated as defined in ASN.1 DER length encoding
         *
         * @return value with added tag and length
         */
        get() {
            return ASN1Encoder.getTlv(this.tag, this.value)
        }

    companion object {
        fun fromByteArray(derBytes: ByteArray): DerTlv? {
            if (derBytes.isEmpty()) return null

            val tag = derBytes[0]
            val (length, lengthBytes) = decodeDerLength(derBytes, 1)
            val valueBytes = derBytes.copyOfRange(1 + lengthBytes, 1 + lengthBytes + length)

            return DerTlv(tag, valueBytes)
        }

        /**
         * Parses all consecutive DER-TLVs from a ByteArray.
         */
        fun parseAll(rawBytes: ByteArray): List<DerTlv> {
            val result = mutableListOf<DerTlv>()
            var pos = 0
            while (pos < rawBytes.size) {
                val tag = rawBytes[pos]
                pos += 1
                val (length, lengthBytes) = decodeDerLength(rawBytes, pos)
                pos += lengthBytes
                // A truncated TLV or a misaligned stream (e.g. when a wrong header length shifted the
                // start of the message) would otherwise read past the array and throw an opaque
                // IndexOutOfBoundsException. Fail with a clear IllegalArgumentException instead.
                require(pos + length <= rawBytes.size) {
                    "Truncated DER-TLV: tag 0x${(tag.toInt() and 0xFF).toString(16).padStart(2, '0')} " +
                            "claims $length value bytes at offset $pos but only ${rawBytes.size - pos} remain"
                }
                result.add(DerTlv(tag, rawBytes.copyOfRange(pos, pos + length)))
                pos += length
            }
            return result
        }

        /**
         * Decodes DER length starting at [offset].
         * @return Pair(decodedLength, numberOfBytesConsumed)
         */
        internal fun decodeDerLength(bytes: ByteArray, offset: Int): Pair<Int, Int> {
            // The length prefix may run past the end of a truncated / misaligned stream. Guard every
            // read so such input fails with a clear IllegalArgumentException instead of an opaque
            // IndexOutOfBoundsException (see parseAll for how misalignment reaches this point).
            require(offset < bytes.size) {
                "Truncated DER-TLV: missing length byte at offset $offset (size ${bytes.size})"
            }
            val firstByte = bytes[offset].toInt() and 0xFF
            if (firstByte <= 0x7F) return Pair(firstByte, 1)
            val numLengthBytes = firstByte - 128
            // More than 4 length bytes cannot fit in a (non-negative) Int and would overflow the
            // accumulator below, so reject it rather than silently producing a bogus/negative length.
            require(numLengthBytes <= 4) {
                "Unsupported DER-TLV length: $numLengthBytes length bytes exceed the 4-byte Int range"
            }
            require(offset + 1 + numLengthBytes <= bytes.size) {
                "Truncated DER-TLV: length field at offset $offset declares $numLengthBytes bytes " +
                        "but only ${bytes.size - offset - 1} remain"
            }
            var length = 0
            for (i in 0 until numLengthBytes) {
                length = (length shl 8) or (bytes[offset + 1 + i].toInt() and 0xFF)
            }
            require(length >= 0) {
                "Unsupported DER-TLV length: value $length exceeds Int.MAX_VALUE at offset $offset"
            }
            return Pair(length, 1 + numLengthBytes)
        }
    }
}
