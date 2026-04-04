package de.tsenger.vdstools

interface Signer {
    val fieldSize: Int
    fun sign(data: ByteArray): ByteArray
}
