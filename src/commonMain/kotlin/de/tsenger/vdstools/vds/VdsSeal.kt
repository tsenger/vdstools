package de.tsenger.vdstools.vds


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.SignatureInfo
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
        // Use effectiveVdsType which considers extended message definition resolution
        this.documentType = vdsMessageGroup.effectiveVdsType
    }

    constructor(vdsHeader: VdsHeader, vdsMessageGroup: VdsMessageGroup, signer: Signer) {
        this.vdsHeader = vdsHeader
        this.vdsMessageGroup = vdsMessageGroup
        this.vdsSignature = createVdsSignature(vdsHeader, vdsMessageGroup, signer)
        this.documentType = vdsHeader.vdsType
    }

    override val documentType: String

    override val issuingCountry: String
        get() = vdsHeader.issuingCountry

    val signerCertRef: String
        get() = vdsHeader.signerCertRef

    val signerIdentifier: String?
        get() = vdsHeader.signerIdentifier

    val certificateReference: String?
        get() = vdsHeader.certificateReference

    val issuingDate: LocalDate?
        get() = vdsHeader.issuingDate

    val sigDate: LocalDate?
        get() = vdsHeader.sigDate

    val docFeatureRef: Byte
        get() = vdsHeader.docFeatureRef

    val docTypeCat: Byte
        get() = vdsHeader.docTypeCat

    override val signedBytes: ByteArray
        get() = vdsHeader.encoded + vdsMessageGroup.encoded

    val encoded: ByteArray
        get() = vdsHeader.encoded + vdsMessageGroup.encoded + (vdsSignature?.encoded ?: byteArrayOf())

    val signatureBytes: ByteArray
        get() = vdsSignature?.plainSignatureBytes ?: byteArrayOf()

    override val rawString: String
        get() = DataEncoder.encodeBase256(encoded)

    val vdsMessageList: List<VdsMessage>
        get() = vdsMessageGroup.messageList


    private fun createVdsSignature(vdsHeader: VdsHeader, vdsMessageGroup: VdsMessageGroup, signer: Signer): VdsSignature? {
        val headerMessage = vdsHeader.encoded + vdsMessageGroup.encoded
        try {
            val signatureBytes = signer.sign(headerMessage)
            return VdsSignature(signatureBytes)
        } catch (e: Exception) {
            log.e("Signature creation failed: " + e.message)
            return null
        }
    }

    override fun getMessage(name: String): Message? {
        val vdsMessage = vdsMessageGroup.getMessage(name) ?: return null
        val mrzLength = getMrzLength(vdsMessage.name)
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding, mrzLength)
        )
    }

    override fun getMessage(tag: Int): Message? {
        val vdsMessage = vdsMessageGroup.getMessage(tag) ?: return null
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
                signerCertificateReference = signerCertRef,
                signingDate = sigDate ?: LocalDate(1970, 1, 1),
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

    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")
        fun fromRawString(rawString: String): Seal {
            var seal: Seal? = null
            try {
                seal = parseVdsSeal(DataEncoder.decodeBase256(rawString))
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
            return seal!!
        }

        fun fromByteArray(rawBytes: ByteArray): Seal {
            var seal: Seal? = null
            try {
                seal = parseVdsSeal(rawBytes)
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
            return seal!!
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun parseVdsSeal(rawBytes: ByteArray): Seal {
            val rawDataBuffer = Buffer().write(rawBytes)
            log.v("rawData: ${rawBytes.toHexString()}")

            val vdsHeader = VdsHeader.fromBuffer(rawDataBuffer)
            var vdsSignature: VdsSignature? = null

            val derTlvList = DataEncoder
                .parseDerTLvs(rawDataBuffer.readByteArray())

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
            if (DataEncoder.requiresUuidLookup(vdsHeader.vdsType)) {
                val uuidTag = DataEncoder.getUuidMessageTag(vdsHeader.vdsType)
                vdsMessageGroup.resolveExtendedMessageDefinition(uuidTag)
                log.d("Resolved effectiveVdsType: ${vdsMessageGroup.effectiveVdsType}")
            }

            return VdsSeal(vdsHeader, vdsMessageGroup, vdsSignature)
        }
    }
}
