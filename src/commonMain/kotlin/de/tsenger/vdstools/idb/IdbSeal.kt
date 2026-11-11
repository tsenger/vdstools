package de.tsenger.vdstools.idb

import de.tsenger.vdstools.Base32
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SealType
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.internal.logW
import kotlinx.datetime.LocalDate

class IdbSeal : Seal {
    private var barcodeFlag: Char = 0x41.toChar()
    private var _barcodeIdentifier: String = BARCODE_IDENTIFIER
    internal val payLoad: IdbPayload

    internal constructor(barcodeFlag: Char, barcodePayload: IdbPayload, identifier: String = BARCODE_IDENTIFIER) {
        this.barcodeFlag = barcodeFlag
        this._barcodeIdentifier = identifier
        this.payLoad = barcodePayload
        // bind the national document type; document types loaded later do not change this seal
        this.documentType = bindDocumentType()
        this.nationalFindings = nationalDocumentTypeFindings()
    }

    internal constructor(isSigned: Boolean, isZipped: Boolean, barcodePayload: IdbPayload) {
        if (isSigned) barcodeFlag = (barcodeFlag.code + 0x01).toChar()
        if (isZipped) barcodeFlag = (barcodeFlag.code + 0x02).toChar()
        this.payLoad = barcodePayload
        this.documentType = bindDocumentType()
        this.nationalFindings = nationalDocumentTypeFindings()
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

    override val sealType = SealType.IDB

    /**
     * The name of the national document type if the message group contains a message whose type
     * identifies it (`identifiesDocumentType`, e.g. BSI TR-03137 0x86 `NATIONAL_DOCUMENT_IDENTIFIER`).
     * Otherwise, the comma separated names of the messages in the message group.
     */
    override val documentType: String

    /** Findings of the national document type, bound with [documentType] when the seal is created. */
    private val nationalFindings: List<FormatFinding>

    /** The value of the message that identifies the national document type, null if there is none. */
    private fun documentTypeReference(): Int? {
        val (message, _) = payLoad.idbMessageGroup.documentTypeIdentifier ?: return null
        return (message.value as? MessageValue.ByteValue)?.value
    }

    private fun bindDocumentType(): String {
        val docTypeId = documentTypeReference() ?: return messageList.joinToString(", ") { it.name }
        return DataEncoder.idbDocumentTypes.getDocumentType(docTypeId)
    }


    override val messageList: List<Message>
        get() = payLoad.idbMessageGroup.messageList.map { idbMessage ->
            Message(idbMessage.tag, idbMessage.name, idbMessage.coding, idbMessage.value, idbMessage.messageList)
        }

    override val formatFindings: List<FormatFinding>
        get() = nationalFindings + payLoad.idbMessageGroup.formatFindings()

    /**
     * Checks the messages against the national document type (see [documentTypeReference], e.g.
     * BSI TR-03137 chapters 12–17), only if it is verified against a `specification`: required message
     * types are missing, others are not allowed if `additionalMessagesAllowed` is false.
     */
    private fun nationalDocumentTypeFindings(): List<FormatFinding> {
        val messages = messageList
        val docTypeId = documentTypeReference() ?: return emptyList()
        val documentType = DataEncoder.idbDocumentTypes.findDocumentType(docTypeId)
            ?: return listOf(FormatFinding.UnknownDocumentType(docTypeId))
        if (documentType.specification == null) return emptyList()
        val expected = documentType.messages
        val present = messages.filter { it.isDefined }
        val findings = mutableListOf<FormatFinding>()
        for (ref in expected.filter { it.required }) {
            if (present.none { it.name.equals(ref.name, ignoreCase = true) }) {
                val tag = DataEncoder.idbMessageTypes.getMessageType(ref.name)
                findings += FormatFinding.MissingRequired(listOfNotNull(tag), ref.name)
            }
        }
        if (documentType.additionalMessagesAllowed) return findings
        for (message in present) {
            if (expected.none { it.name.equals(message.name, ignoreCase = true) }) {
                findings += FormatFinding.NotAllowed(listOf(message.tag), message.name)
            }
        }
        return findings
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

    class Builder() {
        private var countryIdentifier: String = "UTO"
        private var certificateReference: ByteArray? = null
        private var signingDate: String? = null
        private var signerCertBytes: ByteArray? = null
        private var compress: Boolean = false
        private val messageBuilder = IdbMessageGroup.Builder()

        /** The message that identifies the document type and its name, appended by [build]; null without document type. */
        private var documentTypeIdentifier: Pair<String, DerTlv>? = null

        /**
         * Builds a seal of a national IDB document type, e.g. `CERTIFYING_PERMANENT_RESIDENCE`. The message
         * that identifies the document type (the message type marked `identifiesDocumentType`, BSI
         * TR-03137: 0x86 `NATIONAL_DOCUMENT_IDENTIFIER`) is appended by [build]; do not add it.
         *
         * @throws IllegalArgumentException if no IDB document type has this name, or not exactly one
         *   message type is marked `identifiesDocumentType`, or that type is not coded as `BYTE`
         */
        constructor(documentType: String) : this() {
            val type = DataEncoder.idbDocumentTypes.findByName(documentType)
                ?: throw IllegalArgumentException("Unknown IDB document type '$documentType'")
            val identifiers = DataEncoder.idbMessageTypes.documentTypeIdentifiers()
            val identifier = identifiers.singleOrNull() ?: throw IllegalArgumentException(
                "Building '$documentType' needs exactly one IDB message type marked identifiesDocumentType, " +
                        "found ${identifiers.map { it.name }}"
            )
            require(identifier.coding == MessageCoding.BYTE) {
                "The document type identifier ${identifier.name} must be coded as BYTE, not ${identifier.coding}"
            }
            documentTypeIdentifier = identifier.name to DerTlv(
                identifier.tag.toByte(), DataEncoder.encodeValueByCoding(identifier.coding, type.tag, identifier.tag)
            )
        }

        /**
         * Three-letter code of the issuing state, e.g. "D<<". Defaults to "UTO" (Utopia, the code ICAO
         * Doc 9303-3 recommends for specimen documents), so a seal built without it is a specimen.
         */
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

        private fun buildMessageGroup(): IdbMessageGroup {
            val (name, identifier) = documentTypeIdentifier ?: return messageBuilder.build()
            require(messageBuilder.derTlvList.none { it.tag == identifier.tag }) {
                "$name is set by the document type; do not add it"
            }
            return IdbMessageGroup(messageBuilder.derTlvList + identifier)
        }

        fun build(): IdbSeal {
            val header = IdbHeader(countryIdentifier)
            val payload = IdbPayload(header, buildMessageGroup(), null, null)
            return IdbSeal(isSigned = false, isZipped = compress, barcodePayload = payload)
        }

        fun build(signer: Signer): IdbSeal {
            val algo = IdbSignatureAlgorithm.fromFieldSize(signer.fieldSize)
            val header = IdbHeader(countryIdentifier, algo, certificateReference, signingDate)
            val msgGroup = buildMessageGroup()
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
