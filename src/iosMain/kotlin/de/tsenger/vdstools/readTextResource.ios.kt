package de.tsenger.vdstools

import de.tsenger.vdstools.internal.logD
import de.tsenger.vdstools.internal.logE
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
 * Finds resource in the vdstools framework bundle.
 *
 * For Kotlin/Native frameworks, resources are bundled in the framework's Resources directory.
 * We need to search in all loaded bundles to find our framework.
 */
internal fun getResourcePath(fileName: String): Path? {
    // Search in all loaded bundles for vdstools framework
    NSBundle.allBundles().forEach { bundle ->
        val bundleBundle = bundle as? NSBundle ?: return@forEach
        val path = bundleBundle.pathForResource(fileName, null)
        if (path != null) {
            logD("getResourcePath", "Found resource at: $path in bundle: ${bundleBundle.bundlePath}")
            return path.toPath()
        }
    }

    logE("getResourcePath", "Resource $fileName not found in any loaded bundle")
    return null
}

