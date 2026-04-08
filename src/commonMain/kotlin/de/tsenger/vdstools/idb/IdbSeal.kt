package de.tsenger.vdstools.idb


import de.tsenger.vdstools.Base32
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SealType
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.internal.logW
import kotlinx.datetime.LocalDate

class IdbSeal : Seal {
    private var barcodeFlag: Char = 0x41.toChar()
    private var _barcodeIdentifier: String = BARCODE_IDENTIFIER
    internal var payLoad: IdbPayload

    internal constructor(barcodeFlag: Char, barcodePayload: IdbPayload, identifier: String = BARCODE_IDENTIFIER) {
        this.barcodeFlag = barcodeFlag
        this._barcodeIdentifier = identifier
        this.payLoad = barcodePayload
    }

    internal constructor(isSigned: Boolean, isZipped: Boolean, barcodePayload: IdbPayload) {
        if (isSigned) barcodeFlag = (barcodeFlag.code + 0x01).toChar()
        if (isZipped) barcodeFlag = (barcodeFlag.code + 0x02).toChar()
        this.payLoad = barcodePayload
    }

    @OptIn(ExperimentalStdlibApi::class)
    override val signerCertReference get() = payLoad.idbHeader.certificateReference?.toHexString()

    override val signingDate: LocalDate?
        get() = try {
            payLoad.idbHeader.getSignatureCreationDate()?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }

    val barcodeIdentifier: String
        get() = _barcodeIdentifier

    val barcodeFlagByte: Byte
        get() = barcodeFlag.code.toByte()

    val isSigned: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x01) == 0x01

    val isZipped: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x02) == 0x02

    override val encoded: ByteArray
        get() = payLoad.idbHeader.encoded +
                payLoad.idbMessageGroup.encoded +
                (payLoad.idbSignerCertificate?.encoded ?: byteArrayOf()) +
                (payLoad.idbSignature?.encoded ?: byteArrayOf())

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


    override fun getMessageByName(name: String): Message? {
        val idbMessage = payLoad.idbMessageGroup.getMessageByName(name) ?: return null
        return Message(idbMessage.tag, idbMessage.name, idbMessage.coding, idbMessage.value, idbMessage.messageList)
    }

    override fun getMessageByTag(tag: Int): Message? {
        val idbMessage = payLoad.idbMessageGroup.getMessageByTag(tag) ?: return null
        return Message(idbMessage.tag, idbMessage.name, idbMessage.coding, idbMessage.value, idbMessage.messageList)
    }

    /**
     * If messageGroup contains a message with tag 0x86 ("NATIONAL_DOCUMENT_IDENTIFIER")
     * the name of the nation document type will be returned. Otherwise, the comma separated name(s) of the
     * available messages in the message group will be returned.
     */
    override val sealType = SealType.IDB
    override val documentType: String
        get() {
            val msg = getMessageByTag(0x86)
            val docTypeId = (msg?.value as? MessageValue.ByteValue)?.value
            return if (docTypeId != null) {
                DataEncoder.idbDocumentTypes.getDocumentType(docTypeId)
            } else messageList.joinToString(", ") { it.name }
        }

    override val messageList: List<Message>
        get() = payLoad.idbMessageGroup.messageList.map { idbMessage ->
            Message(idbMessage.tag, idbMessage.name, idbMessage.coding, idbMessage.value, idbMessage.messageList)
        }

    @OptIn(ExperimentalStdlibApi::class)
    override val signatureInfo: SignatureInfo?
        get() {
            val idbSig = payLoad.idbSignature
            if (!isSigned || idbSig == null) return null
            var sigDate = LocalDate(1970, 1, 1)
            try {
                sigDate = LocalDate.parse(payLoad.idbHeader.getSignatureCreationDate() ?: "1970-01-01")
            } catch (_: IllegalArgumentException) {
            }
            return SignatureInfo(
                plainSignatureBytes = idbSig.plainSignatureBytes,
                signerCertificateReference = payLoad.idbHeader.certificateReference?.toHexString() ?: "",
                signingDate = sigDate,
                signedBytes = payLoad.idbHeader.encoded + payLoad.idbMessageGroup.encoded,
                signerCertificateBytes = null,
                signatureAlgorithm = payLoad.idbHeader.getSignatureAlgorithm()?.name
            )
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

    class Builder {
        private var countryIdentifier: String = "UTO"
        private var certificateReference: ByteArray? = null
        private var signingDate: String? = null
        private var signerCertBytes: ByteArray? = null
        private var compress: Boolean = false
        private val messageBuilder = IdbMessageGroup.Builder()

        fun countryIdentifier(v: String) = apply { countryIdentifier = v }
        fun certificateReference(v: ByteArray) = apply { certificateReference = v }
        fun signingDate(v: String) = apply { signingDate = v }
        fun signerCertificate(v: ByteArray) = apply { signerCertBytes = v }
        fun compress(v: Boolean) = apply { compress = v }
        fun <T> addMessage(tag: Int, value: T) = apply { messageBuilder.addMessage(tag, value) }
        fun <T> addMessage(name: String, value: T) = apply { messageBuilder.addMessage(name, value) }
        fun addMessage(name: String, block: SubMessageBuilder.() -> Unit) = apply {
            messageBuilder.addMessage(name) { SubMessageBuilder(this).block() }
        }
        fun addMessage(tag: Int, block: SubMessageBuilder.() -> Unit) = apply {
            messageBuilder.addMessage(tag) { SubMessageBuilder(this).block() }
        }

        fun build(): IdbSeal {
            val header = IdbHeader(countryIdentifier)
            val payload = IdbPayload(header, messageBuilder.build(), null, null)
            return IdbSeal(isSigned = false, isZipped = compress, barcodePayload = payload)
        }

        fun build(signer: Signer): IdbSeal {
            val algo = IdbSignatureAlgorithm.fromFieldSize(signer.fieldSize)
            val header = IdbHeader(countryIdentifier, algo, certificateReference, signingDate)
            val msgGroup = messageBuilder.build()
            val sigBytes = signer.sign(header.encoded + msgGroup.encoded)
            val cert = signerCertBytes?.let { IdbSignerCertificate(it) }
            val payload = IdbPayload(header, msgGroup, cert, IdbSignature(sigBytes))
            return IdbSeal(isSigned = true, isZipped = compress, barcodePayload = payload)
        }

        class SubMessageBuilder internal constructor(private val inner: IdbMessageGroup.Builder) {
            fun <T> addMessage(tag: Int, value: T) = apply { inner.addMessage(tag, value) }
            fun <T> addMessage(name: String, value: T) = apply { inner.addMessage(name, value) }
        }
    }

    companion object {
        const val BARCODE_IDENTIFIER_OLD: String = "NDB1"
        const val BARCODE_IDENTIFIER: String = "RDB1"

        private const val TAG = "IdbSeal"

        @Throws(IllegalArgumentException::class)
        internal fun fromString(barcodeString: String): Seal {
            val strBuffer = StringBuilder(barcodeString)

            val barcodeIdentifier = strBuffer.substring(0, 4)
            val isIcaoBarcode = barcodeIdentifier == BARCODE_IDENTIFIER || barcodeIdentifier == BARCODE_IDENTIFIER_OLD

            if (!isIcaoBarcode) {
                throw IllegalArgumentException("Didn't found an ICAO Barcode in the given String: $barcodeString")
            }
            if (barcodeIdentifier == BARCODE_IDENTIFIER_OLD) {
                logW(TAG, "Using old ICAO barcode identifier NDB instead of new identifier RDB!")
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
            return IdbSeal(barcodeFlag, payload, barcodeIdentifier)
        }
    }
}
