package de.tsenger.vdstools

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath


@Throws(FileNotFoundException::class)
actual fun readTextResource(fileName: String): String {
    val fileSystem = FileSystem.SYSTEM
    return getResourcePath(fileName)?.let { path ->
        return fileSystem.read(path) {
            readUtf8()
        }
    } ?: throw FileNotFoundException("File $fileName not found in resources!")
}

/**
 * Ermittelt den Pfad zur Ressource auf der JVM.
 */
internal fun getResourcePath(fileName: String): Path? {
    return ClassLoader.getSystemResource(fileName)?.toURI()?.path?.toPath()
}
