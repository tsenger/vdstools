package de.tsenger.vdstools.vds.tr03171

/**
 * Version of BSI TR-03171 a document profile conforms to.
 *
 * Determines the base document type a profile is carried by: v0.8 profiles use document category
 * 0xC8 (`ADMINISTRATIVE_DOCUMENTS_V8`), v0.9 profiles use 0xC9 (`ADMINISTRATIVE_DOCUMENTS_V9`).
 */
enum class TrVersion {
    V0_8,
    V0_9
}
