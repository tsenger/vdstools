package de.tsenger.vdstools.fr2ddoc

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo


class Fr2ddocSeal private constructor(
    private val fr2ddocHeader: Fr2ddocHeader,
    private val fr2ddocMessageGroup: Fr2ddocMessageGroup,
    private val fr2ddocSignature: Fr2ddocSignature,
    private val barcodeString: String,
    private val signedData: ByteArray
) : Seal() {

    override val signerCertReference: String
        get() = (fr2ddocHeader.signerIdentifier ?: "") + (fr2ddocHeader.certificateReference ?: "")

    override val signingDate get() = fr2ddocHeader.sigDate

    override val documentType: String
        get() = fr2ddocHeader.docType ?: ""

    override val issuingCountry: String
        get() = fr2ddocHeader.issuingCountry ?: ""

    override val messageList: List<Message>
        get() = fr2ddocMessageGroup.messages

    override fun getMessageByName(name: String): Message? =
        fr2ddocMessageGroup.messages.find { it.name == name }

    override fun getMessageByTag(tag: Int): Message? =
        getMessageByTag(tag.toString(16).uppercase().padStart(2, '0'))

    override fun getMessageByTag(tag: String): Message? =
        fr2ddocMessageGroup.messages.find { it.tag == tag }

    override val signatureInfo: SignatureInfo?
        get() {
            val sigDate = fr2ddocHeader.sigDate ?: return null
            return SignatureInfo(
                plainSignatureBytes = fr2ddocSignature.plainSignatureBytes,
                signerCertificateReference = (fr2ddocHeader.signerIdentifier
                    ?: "") + (fr2ddocHeader.certificateReference ?: ""),
                signingDate = sigDate,
                signedBytes = signedData
            )
        }

    override val encoded: ByteArray
        get() = barcodeString.encodeToByteArray()

    override val rawString: String
        get() = barcodeString

    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")

        fun fromRawString(rawString: String): Seal {
            log.v("rawData: $rawString")
            try {
                return parseSeal(rawString)
            } catch (e: Exception) {
                log.e(e.message.toString())
                throw e
            }
        }

        private fun parseSeal(barcodeString: String): Seal {
            val strBuffer = BufferReader(barcodeString)

            val fr2ddocHeader = Fr2ddocHeader.fromStringBuffer(strBuffer)
            val headerLength = strBuffer.pointer

            val remaining = barcodeString.substring(headerLength)
            val usIndex = remaining.indexOf('\u001F')
            if (usIndex < 0) {
                throw IllegalArgumentException("No US separator (0x1F) found in 2D-DOC barcode")
            }

            val dataString = remaining.substring(0, usIndex)
            val signatureString = remaining.substring(usIndex + 1)

            log.v { "dataString: $dataString" }
            log.v { "signatureString: $signatureString" }

            val perimeterId = fr2ddocHeader.perimeterId ?: "1"
            val messageGroup = Fr2ddocMessageGroup.parse(dataString, perimeterId)
            val signature = Fr2ddocSignature.fromString(signatureString)

            // signedBytes = header + dataString (everything before US)
            val signedData = barcodeString.substring(0, headerLength + usIndex).encodeToByteArray()

            return Fr2ddocSeal(fr2ddocHeader, messageGroup, signature, barcodeString, signedData)
        }
    }

    class BufferReader(val data: String) {
        var pointer = 0
        fun next(n: Int) = data.substring(pointer, pointer + n).also { pointer += n }
    }
}
