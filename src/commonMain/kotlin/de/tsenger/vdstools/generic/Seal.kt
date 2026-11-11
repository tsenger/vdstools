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
    /**
     * All messages of the seal in seal order, except metadata messages ([metadataMessageList]).
     * A tag without definition is not dropped but returned raw ([Message.isRaw]), named by its tag
     * (e.g. `0x0A`); the same applies to a value that cannot be decoded with its coding.
     */
    abstract val messageList: List<Message>

    /**
     * Messages of the tags a document type declares as metadata (e.g. the TR-03171 profile number
     * and URIs), resolved like [messageList]. Empty for document types without metadata tags.
     */
    open val metadataMessageList: List<Message>
        get() = emptyList()
    abstract val issuingCountry: String
    abstract val signatureInfo: SignatureInfo?

    /**
     * Findings of the format validation (ICAO Doc 9303-13 Appendix D, step 1), without judgement.
     * Empty if nothing was noticed. Signature and certificate are not covered.
     */
    open val formatFindings: List<FormatFinding>
        get() = emptyList()

    open val signedBytes: ByteArray?
        get() = signatureInfo?.signedBytes
    abstract val encoded: ByteArray
    abstract val rawString: String
}
