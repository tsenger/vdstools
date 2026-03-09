package de.tsenger.vdstools.generic

import de.tsenger.vdstools.fr2ddoc.Fr2ddocSeal
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsSeal

abstract class Seal {
    abstract fun getMessageByName(name: String): Message?
    abstract fun getMessageByTag(tag: Int): Message?
    abstract fun getMessageByTag(tag: String): Message?

    @Deprecated("Use getMessageByName(name) instead", ReplaceWith("getMessageByName(name)"))
    fun getMessage(name: String): Message? = getMessageByName(name)

    @Deprecated("Use getMessageByTag(tag) instead", ReplaceWith("getMessageByTag(tag)"))
    fun getMessage(tag: Int): Message? = getMessageByTag(tag)

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
    abstract val signedBytes: ByteArray?
    abstract val encoded: ByteArray
    abstract val rawString: String


    companion object {
        fun fromString(input: String): Seal {
            return when {
                input.startsWith(IdbSeal.BARCODE_IDENTIFIER) -> IdbSeal.fromString(input)
                input.startsWith(IdbSeal.BARCODE_IDENTIFIER_OLD) -> IdbSeal.fromString(input)
                input.startsWith("Ü") -> VdsSeal.fromRawString(input)
                input.startsWith("DC") -> Fr2ddocSeal.fromRawString(input)
                else -> throw IllegalArgumentException("can't parse given input to a known seal type: $input")
            }
        }
    }
}