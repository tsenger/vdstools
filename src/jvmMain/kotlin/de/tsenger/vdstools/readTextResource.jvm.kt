package de.tsenger.vdstools

import okio.FileNotFoundException
import java.io.BufferedReader
import java.io.InputStream


@Throws(FileNotFoundException::class)
actual fun readTextResource(fileName: String): String {
    val inputStream: InputStream? = object {}.javaClass.classLoader.getResourceAsStream(fileName)

    if (inputStream == null) throw FileNotFoundException("File $fileName not found in resources!")
    val reader: BufferedReader? = null
    var content: String
    try {
        val reader = BufferedReader(inputStream.reader())
        content = reader.readText()
    } finally {
        reader?.close()
    }
    return content
}
