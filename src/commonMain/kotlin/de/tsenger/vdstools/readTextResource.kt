package de.tsenger.vdstools

import okio.FileNotFoundException

const val DEFAULT_SEAL_CODINGS: String = "SealCodings.json"
const val DEFAULT_IDB_MESSAGE_TYPES: String = "IdbMessageTypes.json"
const val DEFAULT_IDB_DOCUMENT_TYPES: String = "IdbNationalDocumentTypes.json"


@Throws(FileNotFoundException::class)
expect fun readTextResource(fileName: String): String

