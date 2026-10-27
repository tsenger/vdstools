package de.tsenger.vdstools.internal

/**
 * Bit size of the curve whose values (private key, point coordinate, signature component r or s)
 * are encoded with [componentLength] bytes, or null if no supported curve uses this length.
 */
internal fun ecFieldBitSize(componentLength: Int): Int? = when (componentLength) {
    28 -> 224
    32 -> 256
    40 -> 320
    48 -> 384
    64 -> 512
    66 -> 521
    else -> null
}
