package de.tsenger.vdstools


import kotlinx.io.files.FileNotFoundException
import java.io.BufferedReader
import java.io.InputStream


@Throws(FileNotFoundException::class)
actual fun readTextResource(fileName: String): String {
    val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(fileName)
        ?: throw FileNotFoundException("File $fileName not found in resources!")

    BufferedReader(inputStream.reader()).use {
        return it.readText()
    }

}
