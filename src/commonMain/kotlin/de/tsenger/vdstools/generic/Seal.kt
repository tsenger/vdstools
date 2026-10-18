package de.tsenger.vdstools.generic

import kotlinx.datetime.LocalDate

abstract class Seal {
    abstract val signerCertReference: String?
    abstract val signingDate: LocalDate?

    /**
     * Looks up a message by name, ignoring case. Names are taken verbatim from the definition:
     * the bundled JSON registries use `UPPER_SNAKE_CASE` (e.g. `SURNAME`), TR-03171 XML profiles
     * use whatever the profile's `<name>` says (e.g. `familyName` or `Familienname`).
     */
    abstract fun getMessageByName(name: String): Message?
    abstract fun getMessageByTag(tag: Int): Message?

    abstract val sealType: SealType
    abstract val documentType: String

    open val baseDocumentType: String?
        get() = null
    open val documentProfileUuid: ByteArray?
        get() = null
    abstract val messageList: List<Message>
    open val metadataMessageList: List<Message>
        get() = emptyList()
    abstract val issuingCountry: String
    abstract val signatureInfo: SignatureInfo?

    open val signedBytes: ByteArray?
        get() = signatureInfo?.signedBytes
    abstract val encoded: ByteArray
    abstract val rawString: String
}
