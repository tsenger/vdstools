package de.tsenger.vdstools.vds


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.*
import kotlinx.datetime.LocalDate
import okio.Buffer


class VdsSeal : Seal {
    private val log = Logger.withTag(this::class.simpleName ?: "")

    private val vdsHeader: VdsHeader
    private val vdsMessageGroup: VdsMessageGroup
    private val vdsSignature: VdsSignature?

    private constructor(vdsHeader: VdsHeader, vdsMessageGroup: VdsMessageGroup, vdsSignature: VdsSignature?) {
        this.vdsHeader = vdsHeader
        this.vdsMessageGroup = vdsMessageGroup
        this.vdsSignature = vdsSignature
        this.documentType = vdsMessageGroup.profileDefinition?.definitionName ?: vdsHeader.vdsType
    }

    internal constructor(vdsHeader: VdsHeader, vdsMessageGroup: VdsMessageGroup, signer: Signer) {
        this.vdsHeader = vdsHeader
        this.vdsMessageGroup = vdsMessageGroup
        this.vdsSignature = createVdsSignature(vdsHeader, vdsMessageGroup, signer)
        this.documentType = vdsMessageGroup.profileDefinition?.definitionName ?: vdsHeader.vdsType
    }

    override val signerCertReference: String
        get() = vdsHeader.signerCertRef

    override val signingDate get() = vdsHeader.sigDate

    override val sealType = SealType.VDS
    override val documentType: String

    override val baseDocumentType: String?
        get() = vdsMessageGroup.profileDefinition?.baseDocumentType

    override val documentProfileUuid: ByteArray?
        get() = vdsMessageGroup.documentProfileUuid

    override val issuingCountry: String
        get() = vdsHeader.issuingCountry

    val signerIdentifier: String?
        get() = vdsHeader.signerIdentifier

    val certificateReference: String?
        get() = vdsHeader.certificateReference

    val issuingDate: LocalDate?
        get() = vdsHeader.issuingDate

    val docFeatureRef: Byte
        get() = vdsHeader.docFeatureRef

    val docTypeCat: Byte
        get() = vdsHeader.docTypeCat

    val headerBytes: ByteArray
        get() = vdsHeader.encoded

    override val encoded: ByteArray
        get() = vdsHeader.encoded + vdsMessageGroup.encoded + (vdsSignature?.encoded ?: byteArrayOf())

    val signatureBytes: ByteArray
        get() = vdsSignature?.plainSignatureBytes ?: byteArrayOf()

    override val rawString: String
        get() = DataEncoder.encodeBase256(encoded)


    private fun createVdsSignature(
        vdsHeader: VdsHeader,
        vdsMessageGroup: VdsMessageGroup,
        signer: Signer
    ): VdsSignature? {
        val headerMessage = vdsHeader.encoded + vdsMessageGroup.encoded
        try {
            val signatureBytes = signer.sign(headerMessage)
            return VdsSignature(signatureBytes)
        } catch (e: Exception) {
            log.e("Signature creation failed: " + e.message)
            return null
        }
    }

    override fun getMessageByName(name: String): Message? {
        val vdsMessage = vdsMessageGroup.getMessageByName(name) ?: return null
        val mrzLength = getMrzLength(vdsMessage.name)
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
        )
    }

    override fun getMessageByTag(tag: Int): Message? {
        val vdsMessage = vdsMessageGroup.getMessageByTag(tag) ?: return null
        val mrzLength = getMrzLength(vdsMessage.name)
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
        )
    }

    override fun getMessageByTag(tag: String): Message? {
        val vdsMessage = vdsMessageGroup.getMessageByTag(tag) ?: return null
        val mrzLength = getMrzLength(vdsMessage.name)
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
        )
    }

    private fun getMrzLength(messageName: String): Int? = when (messageName) {
        "MRZ_MRVA" -> 88
        "MRZ_MRVB" -> 72
        else -> null
    }

    override val signatureInfo: SignatureInfo?
        get() {
            if (signatureBytes.isEmpty()) return null
            val fieldSize = signatureBytes.size * 4
            val signatureAlgorithm = when (fieldSize) {
                in Int.MIN_VALUE..224 -> "SHA224_WITH_ECDSA"
                in 225..256 -> "SHA256_WITH_ECDSA"
                in 257..384 -> "SHA384_WITH_ECDSA"
                in 385..512 -> "SHA512_WITH_ECDSA"
                else -> ""
            }
            return SignatureInfo(
                plainSignatureBytes = signatureBytes,
                signerCertificateReference = signerCertReference,
                signingDate = signingDate ?: LocalDate(1970, 1, 1),
                signedBytes = vdsHeader.encoded + vdsMessageGroup.encoded,
                signerCertificateBytes = null,
                signatureAlgorithm = signatureAlgorithm,
            )
        }

    override val messageList: List<Message>
        get() = vdsMessageGroup.messageList.map { vdsMessage ->
            val mrzLength = getMrzLength(vdsMessage.name)
            Message(
                vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
                MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
            )
        }

    override val metadataMessageList: List<Message>
        get() = vdsMessageGroup.metadataMessageList.map { vdsMessage ->
            val mrzLength = getMrzLength(vdsMessage.name)
            Message(
                vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
                MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
            )
        }

    class Builder(private val documentType: String) {
        private val headerBuilder = VdsHeader.Builder(documentType)
        private val messageBuilder = VdsMessageGroup.Builder(documentType)

        fun issuingCountry(v: String) = apply { headerBuilder.setIssuingCountry(v) }
        fun signerIdentifier(v: String) = apply { headerBuilder.setSignerIdentifier(v) }
        fun certificateReference(v: String) = apply { headerBuilder.setCertificateReference(v) }
        fun issuingDate(v: LocalDate) = apply { headerBuilder.setIssuingDate(v) }
        fun sigDate(v: LocalDate) = apply { headerBuilder.setSigDate(v) }
        fun <T> addMessage(tag: Int, value: T) = apply { messageBuilder.addMessage(tag, value) }
        fun <T> addMessage(name: String, value: T) = apply { messageBuilder.addMessage(name, value) }

        fun build(signer: Signer): VdsSeal = VdsSeal(headerBuilder.build(), messageBuilder.build(), signer)
    }

    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")
        internal fun fromRawString(rawString: String): Seal {
            return parseVdsSeal(DataEncoder.decodeBase256(rawString))
        }

        internal fun fromByteArray(rawBytes: ByteArray): Seal {
            return parseVdsSeal(rawBytes)
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun parseVdsSeal(rawBytes: ByteArray): Seal {
            val rawDataBuffer = Buffer().write(rawBytes)
            log.v("rawData: ${rawBytes.toHexString()}")

            val vdsHeader = VdsHeader.fromBuffer(rawDataBuffer)
            var vdsSignature: VdsSignature? = null

            val derTlvList = DerTlv.parseAll(rawDataBuffer.readByteArray())

            val messageList: MutableList<DerTlv> = ArrayList(derTlvList.size - 1)

            for (derTlv in derTlvList) {
                if (derTlv.tag == 0xff.toByte()) {
                    vdsSignature = VdsSignature.fromByteArray(derTlv.encoded)
                } else {
                    messageList.add(derTlv)
                }
            }
            val vdsMessageGroup = VdsMessageGroup(vdsHeader.vdsType, messageList)

            // Resolve extended message definition if this seal type requires UUID lookup
            if (DataEncoder.vdsDocumentTypes.requiresProfileLookup(vdsHeader.vdsType)) {
                val uuidTag = DataEncoder.vdsDocumentTypes.getUuidMessageTag(vdsHeader.vdsType)
                vdsMessageGroup.resolveProfileDefinition(uuidTag)
                DataEncoder.vdsDocumentTypes.getMetadataTags(vdsHeader.vdsType).forEach {
                    vdsMessageGroup.metadataTags.add(it)
                }
                log.d("Resolved extended definition: ${vdsMessageGroup.profileDefinition?.definitionName}")
            }

            return VdsSeal(vdsHeader, vdsMessageGroup, vdsSignature)
        }
    }
}
