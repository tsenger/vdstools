package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder

/**
 * Version of BSI TR-03171 a document profile conforms to.
 *
 * Determines the base document type a profile is carried by: v0.8 profiles use document category
 * 0xC8 (`ADMINISTRATIVE_DOCUMENTS_V8`), v0.9 profiles use 0xC9 (`ADMINISTRATIVE_DOCUMENTS_V9`).
 */
enum class TrVersion(val baseDocumentType: String) {
    V0_8(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8),
    V0_9(DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9)
}
