package de.tsenger.vdstools.idb


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.Base32
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.vds.FeatureValue
import kotlinx.datetime.LocalDate

class IcaoBarcode : Seal {
    private var barcodeFlag: Char = 0x41.toChar()
    var payLoad: IdbPayload

    constructor(barcodeFlag: Char, barcodePayload: IdbPayload) {
        this.barcodeFlag = barcodeFlag
        this.payLoad = barcodePayload
    }

    constructor(isSigned: Boolean, isZipped: Boolean, barcodePayload: IdbPayload) {
        if (isSigned) barcodeFlag = (barcodeFlag.code + 0x01).toChar()
        if (isZipped) barcodeFlag = (barcodeFlag.code + 0x02).toChar()
        this.payLoad = barcodePayload
    }

    val isSigned: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x01) == 0x01

    val isZipped: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x02) == 0x02

    override val rawString: String
        get() {
            val strBuffer = StringBuilder(BARCODE_IDENTIFIER)
            strBuffer.append(barcodeFlag)

            val payloadBytes: ByteArray = if (isZipped) {
                DataEncoder.zip(payLoad.encoded)
            } else {
                payLoad.encoded
            }

            val base32EncodedPayload = Base32.encode(payloadBytes).replace("=", "")

            strBuffer.append(base32EncodedPayload)
            return strBuffer.toString()
        }


    override fun getMessage(name: String): Message? {
        val idbMessage = payLoad.idbMessageGroup.getMessage(name) ?: return null
        return Message(idbMessage.messageTypeTag, idbMessage.messageTypeName, idbMessage.coding, idbMessage.value)
    }

    override fun getMessage(tag: Int): Message? {
        val idbMessage = payLoad.idbMessageGroup.getMessage(tag) ?: return null
        return Message(idbMessage.messageTypeTag, idbMessage.messageTypeName, idbMessage.coding, idbMessage.value)
    }

    /**
     * If messageGroup contains a message with tag 0x86 ("NATIONAL_DOCUMENT_IDENTIFIER")
     * the name of the nation document type will be returned. Otherwise, the comma separated name(s) of the
     * available messages in the message group will be returned.
     */
    override val documentType: String
        get() {
            val msg = getMessage(0x86)
            val docTypeId = (msg?.value as? FeatureValue.ByteValue)?.value
            return if (docTypeId != null) {
                DataEncoder.getIdbDocumentTypeName(docTypeId)
            } else messageList.joinToString(", ") { it.messageTypeName }
        }

    override val messageList: List<Message>
        get() = payLoad.idbMessageGroup.messagesList.map { idbMessage ->
            Message(idbMessage.messageTypeTag, idbMessage.messageTypeName, idbMessage.coding, idbMessage.value)
        }

    override val signatureInfo: SignatureInfo?
        get() {
            val idbSignature = payLoad.idbSignature
            if (!isSigned || idbSignature == null) return null
            var sigDate = LocalDate(1970, 1, 1)
            try {
                sigDate = LocalDate.parse(payLoad.idbHeader.getSignatureCreationDate() ?: "1970-01-01")
            } catch (_: IllegalArgumentException) {
            }
            return SignatureInfo(
                plainSignatureBytes = idbSignature.plainSignatureBytes,
                signerCertificateReference = payLoad.idbHeader.certificateReference?.toHexString() ?: "",
                signingDate = sigDate,
                signerCertificateBytes = null,
                signatureAlgorithm = payLoad.idbHeader.getSignatureAlgorithm()?.name
            )
        }

    override val signedBytes: ByteArray
        get() = payLoad.idbHeader.encoded + payLoad.idbMessageGroup.encoded


    val signature: IdbSignature?
        get() {
            return payLoad.idbSignature
        }

    override val issuingCountry: String
        get() {
            return payLoad.idbHeader.getCountryIdentifier()
        }

    val signatureAlgorithmName: String?
        get() {
            return payLoad.idbHeader.getSignatureAlgorithm()?.name
        }

    val signatureCreationDate: String?
        get() {
            return payLoad.idbHeader.getSignatureCreationDate()
        }

    companion object {
        const val BARCODE_IDENTIFIER_OLD: String = "NDB1"
        const val BARCODE_IDENTIFIER: String = "RDB1"

        private val log = Logger.withTag(this::class.simpleName ?: "")

        @Throws(IllegalArgumentException::class)
        fun fromString(barcodeString: String): Seal {
            val strBuffer = StringBuilder(barcodeString)

            val barcodeIdentifier = strBuffer.substring(0, 4)
            val isIcaoBarcode = barcodeIdentifier == BARCODE_IDENTIFIER || barcodeIdentifier == BARCODE_IDENTIFIER_OLD

            if (!isIcaoBarcode) {
                throw IllegalArgumentException("Didn't found an ICAO Barcode in the given String: $barcodeString")
            }
            if (barcodeIdentifier == BARCODE_IDENTIFIER_OLD) {
                log.w { "Using old ICAO barcode identifier NDB instead of new identifier RDB!" }
            }

            val barcodeFlag = strBuffer[4]
            val isSigned = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x01) == 0x01
            val isZipped = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x02) == 0x02

            val base32EncodedPayload = StringBuilder(strBuffer.substring(5))
            while (base32EncodedPayload.length % 8 != 0) {
                base32EncodedPayload.append("=")
            }

            var payloadBytes = Base32.decode(base32EncodedPayload.toString())

            if (isZipped) {
                payloadBytes = DataEncoder.unzip(payloadBytes)
            }

            val payload = IdbPayload.fromByteArray(payloadBytes, isSigned)
            return IcaoBarcode(barcodeFlag, payload)
        }
    }
}
