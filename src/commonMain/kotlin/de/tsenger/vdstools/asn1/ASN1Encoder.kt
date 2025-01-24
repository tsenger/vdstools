package de.tsenger.vdstools.asn1

object ASN1Encoder {


    /**
     * Encodes an Int or ByteArray as an DER coded INTEGER.
     * Handles leading zero bytes and ensures the integer is positive.
     */
    fun getDerInteger(value: Any): ByteArray {
        val byteArray = when (value) {
            is Int -> value.toByteArray()
            is ByteArray -> value
            else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
        }
        // Ensure the integer is positive by prepending a zero byte if needed
        val positiveValue = if (byteArray.isNotEmpty() && byteArray[0].toInt() < 0) {
            byteArrayOf(0) + byteArray
        } else {
            byteArray.dropWhile { it == 0.toByte() }.toByteArray()
        }
        return getTlv(0x02, positiveValue)
    }

    fun getTlv(tag: Byte, bytes: ByteArray): ByteArray {
        val length = getDerLength(bytes.size)
        return byteArrayOf(tag) + length + bytes
    }

    /**
     * Encodes a sequence of ByteArrays which shall contains encoded ASN.1 elements
     * into a DER sequence
     */
    fun getDerSequence(vararg elements: ByteArray): ByteArray {
        val content = elements.flatMap { it.toList() }.toByteArray()
        val length = getDerLength(content.size)
        return byteArrayOf(0x30) + length + content // 0x30 = SEQUENCE tag
    }

    /**
     * Encodes the length according to ASN.1 rules.
     * Uses short form for lengths <= 127 and long form for lengths > 127.
     */
    fun getDerLength(length: Int): ByteArray {
        return if (length <= 127) {
            byteArrayOf(length.toByte()) // Short form
        } else {
            val lengthBytes = length.toByteArray()
            byteArrayOf((0x80 or lengthBytes.size).toByte()) + lengthBytes // Long form
        }
    }

    private fun Int.toByteArray(): ByteArray {
        val result = mutableListOf<Byte>()
        var value = this
        while (value != 0) {
            result.add((value and 0xFF).toByte()) // Nehme die untersten 8 Bits
            value = value ushr 8                // Shift um 8 Bits nach rechts
        }
        return result.reversed().toByteArray()  // Ergebnis umkehren, da das höchstwertige Byte zuerst kommt
    }


}