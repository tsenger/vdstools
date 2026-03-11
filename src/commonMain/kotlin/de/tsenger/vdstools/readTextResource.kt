package de.tsenger.vdstools

import okio.FileNotFoundException

const val DEFAULT_VDS_DOCUMENT_TYPES: String = "VdsDocumentTypes.json"
const val DEFAULT_IDB_MESSAGE_TYPES: String = "IdbMessageTypes.json"
const val DEFAULT_IDB_DOCUMENT_TYPES: String = "IdbGermanDocumentTypes.json"


@Throws(FileNotFoundException::class)
expect fun readTextResource(fileName: String): String