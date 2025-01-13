package de.tsenger.vdstools_mp

import okio.FileNotFoundException

const val DEFAULT_SEAL_CODINGS: String = "SealCodings.json"


expect class FileLoader() {
    @Throws(FileNotFoundException::class)
    fun loadFileFromResources(fileName: String): String
}