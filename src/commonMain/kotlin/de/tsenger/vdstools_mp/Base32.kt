package de.tsenger.vdstools_mp

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val PADDING = '='

    private val CHAR_TO_VALUE = IntArray(256) { -1 }.apply {
        ALPHABET.forEachIndexed { index, char ->
            this[char.code] = index
        }
    }

    fun encode(input: ByteArray): String {
        val output = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        input.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                output.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            output.append(ALPHABET[index])
        }
        while (output.length % 8 != 0) {
            output.append(PADDING)
        }
        return output.toString()
    }

    fun decode(input: String): ByteArray {
        val cleanedInput = input.trimEnd(PADDING)
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        cleanedInput.forEach { char ->
            val value = CHAR_TO_VALUE[char.code]
            if (value == -1) throw IllegalArgumentException("Invalid Base32 character: $char")
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                output.add((buffer shr (bitsLeft - 8)).toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }
}
