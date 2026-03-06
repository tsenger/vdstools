package de.tsenger.vdstools.fr2ddoc

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo


class Fr2ddocSeal : Seal {

    private val header: Fr2ddocHeader
    private val messageGroup: Fr2ddocMessageGroup
    private val signature: Fr2ddocSignature
    private val annex: Fr2ddocAnnex

    private constructor(
        header: Fr2ddocHeader,
        messageGroup: Fr2ddocMessageGroup,
        signature: Fr2ddocSignature,
        annex: Fr2ddocAnnex
    ) {
        this.header = header
        this.messageGroup = messageGroup
        this.signature = signature
        this.annex = annex
    }

    override val documentType: String
        get() {
            TODO()
        }

    override val baseDocumentType: String?
        get() {
            TODO()
        }


    override val documentProfileUuid: ByteArray?
        get() {
            TODO()
        }


    override val issuingCountry: String
        get() {
            TODO()
        }

    override val signedBytes: ByteArray
        get() = TODO()


    override val encoded: ByteArray
        get() = TODO()


    override val rawString: String
        get() = TODO()


    override fun getMessage(name: String): Message? {
        TODO("Not yet implemented")
    }

    override fun getMessage(tag: Int): Message? {
        TODO("Not yet implemented")
    }

    override val messageList: List<Message>
        get() = TODO("Not yet implemented")

    override val signatureInfo: SignatureInfo
        get() = TODO("Not yet implemented")

    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")
        fun fromRawString(rawString: String): Seal {
            var seal: Seal? = null
            log.v("rawData: $rawString")
            try {
                seal = parseSeal(rawString)
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
            return seal!!
        }

        private fun parseSeal(barcodeString: String): Seal {
            val strBuffer = BufferReader(barcodeString)

            val header = Fr2ddocHeader.fromStringBuffer(strBuffer)
            val messageGroup = Fr2ddocMessageGroup.fromStringBuffer(strBuffer)
            val signature = Fr2ddocSignature.fromStringBuffer(strBuffer)
            val annex = Fr2ddocAnnex.fromStringBuffer(strBuffer)

            return Fr2ddocSeal(header, messageGroup, signature, annex)
        }
    }

    class BufferReader(val data: String) {
        var pointer = 0
        fun next(n: Int) = data.substring(pointer, pointer + n).also { pointer += n }
    }
}