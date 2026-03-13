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
            val firstByte = bytes[offset].toInt() and 0xFF
            if (firstByte <= 0x7F) return Pair(firstByte, 1)
            val numLengthBytes = firstByte - 128
            var length = 0
            for (i in 0 until numLengthBytes) {
                length = (length shl 8) or (bytes[offset + 1 + i].toInt() and 0xFF)
            }
            return Pair(length, 1 + numLengthBytes)
        }
    }
}
