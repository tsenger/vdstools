package de.tsenger.vdstools.idb

enum class IdbSignatureAlgorithm(val value: Byte) {
    SHA256_WITH_ECDSA(0x01.toByte()),
    SHA384_WITH_ECDSA(0x02.toByte()),
    SHA512_WITH_ECDSA(0x03.toByte()),
    ;

    companion object {
        private val map = HashMap<Byte, IdbSignatureAlgorithm>()

        init {
            for (algorithm in entries) {
                map[algorithm.value] = algorithm
            }
        }

        fun valueOf(value: Byte): IdbSignatureAlgorithm? {
            return map[value]
        }

        internal fun fromFieldSize(fieldSize: Int): IdbSignatureAlgorithm = when {
            fieldSize <= 256 -> SHA256_WITH_ECDSA
            fieldSize <= 384 -> SHA384_WITH_ECDSA
            else -> SHA512_WITH_ECDSA
        }
    }
}
