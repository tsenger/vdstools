package de.tsenger.vdstools_mp

const val DEFAULT_SEAL_CODINGS: String = "SealCodings.json"

expect class FileLoader() {
    fun loadFileFromResources(fileName: String): String
}