package de.tsenger.vdstools_mp.asn1


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

            var lengthByteCount = 1
            var length: Int = derBytes[1].toInt() and 0xff

            // Überprüfen, ob die Länge mehr als ein Byte erfordert (größer als 127)
            if (length > 127) {
                val lengthOfLength = length - 128
                lengthByteCount += lengthOfLength
                length = 0
                for (i in 2 until 2 + lengthOfLength) {
                    length = length shl 8 or (derBytes[i].toInt() and 0xff)
                }
            }
            // Die Byte-Daten folgen nach der Tag- und Längenangabe
            val valueBytes = derBytes.copyOfRange(1 + lengthByteCount, 1 + lengthByteCount + length)

            return DerTlv(tag, valueBytes)
        }
    }
}
