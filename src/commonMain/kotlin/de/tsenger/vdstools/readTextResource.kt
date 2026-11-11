package de.tsenger.vdstools

import okio.FileNotFoundException

@Throws(FileNotFoundException::class)
expect fun readTextResource(fileName: String): String