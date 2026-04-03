package de.tsenger.vdstools.generic

import kotlinx.datetime.LocalDate

abstract class Seal {
    abstract val signerCertReference: String?
    abstract val signingDate: LocalDate?

    abstract fun getMessageByName(name: String): Message?
    abstract fun getMessageByTag(tag: Int): Message?
    abstract fun getMessageByTag(tag: String): Message?

    @Deprecated("Use getMessageByName(name) instead", ReplaceWith("getMessageByName(name)"))
    fun getMessage(name: String): Message? = getMessageByName(name)

    @Deprecated("Use getMessageByTag(tag) instead", ReplaceWith("getMessageByTag(tag)"))
    fun getMessage(tag: Int): Message? = getMessageByTag(tag)

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
