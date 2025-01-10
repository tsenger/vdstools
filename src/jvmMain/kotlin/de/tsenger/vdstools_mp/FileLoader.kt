package de.tsenger.vdstools_mp

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual class FileLoader {
    actual fun loadFileFromResources(fileName: String): String {
        val fileSystem = FileSystem.SYSTEM
        val path: Path = getResourcePath(fileName)
        return fileSystem.read(path) {
            readUtf8()
        }
    }

    /**
     * Ermittelt den Pfad zur Ressource auf der JVM.
     */
    private fun getResourcePath(fileName: String): Path {
        val resourceUrl = object {}.javaClass.classLoader.getResource(fileName)
            ?: throw FileNotFoundException("Resource not found: $fileName")
        return resourceUrl.toURI().path.toPath()
    }
}