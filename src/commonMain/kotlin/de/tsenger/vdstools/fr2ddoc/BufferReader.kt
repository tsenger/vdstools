package de.tsenger.vdstools.fr2ddoc

internal class BufferReader(val data: String) {
    var pointer = 0
    fun next(n: Int) = data.substring(pointer, pointer + n).also { pointer += n }
}
