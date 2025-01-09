package de.tsenger.vdstools_mp.idb

//@formatter:off
enum class IdbSignatureAlgorithm (val value: Byte) {
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
 }}
