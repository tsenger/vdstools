package de.tsenger.vdstools_mp

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle

actual class FileLoader {
    @Throws(FileNotFoundException::class)
    actual fun loadFileFromResources(fileName: String): String {
        val fileSystem = FileSystem.SYSTEM
        try {
            val path: Path = getResourcePath(fileName)
            return fileSystem.read(path) {
                readUtf8()
            }
        }
        catch (e: IllegalArgumentException) {
            throw FileNotFoundException(e.message)
        }
    }

    /**
     * Ermittelt den Pfad zur Ressource auf iOS.
     */
    private fun getResourcePath(fileName: String): Path {

        val resourcePath = NSBundle.mainBundle.resourcePath
        println("Resource Path: $resourcePath")

        val path = NSBundle.mainBundle.pathForResource(fileName, null)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
        return path.toPath()
    }
}