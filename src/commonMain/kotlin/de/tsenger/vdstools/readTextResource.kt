package de.tsenger.vdstools

import okio.FileNotFoundException

const val DEFAULT_SEAL_CODINGS: String = "SealCodings.json"


@Throws(FileNotFoundException::class)
expect fun readTextResource(fileName: String): String

