package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle


@Throws(FileNotFoundException::class)
actual fun readTextResource(fileName: String): String {
    val fileSystem = FileSystem.SYSTEM
    return getResourcePath(fileName)?.let { path ->
        fileSystem.read(path) {
            readUtf8()
        }
    } ?: throw FileNotFoundException("File $fileName not found in resources!")
}

/**
 * Ermittelt den Pfad zur Ressource auf iOS.
 */
internal fun getResourcePath(fileName: String): Path? {
    val resourcePath = NSBundle.mainBundle.resourcePath
    Logger.withTag("getResourcePath").d("Resource Path: $resourcePath")
    val path = NSBundle.mainBundle.pathForResource(fileName, null)
    return path?.toPath()
}
