package de.tsenger.vdstools.vds


import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.internal.ecFieldBitSize
import de.tsenger.vdstools.internal.logD
import de.tsenger.vdstools.internal.logE
import de.tsenger.vdstools.internal.logV
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.*
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.tr03171.ProfileConverter
import de.tsenger.vdstools.vds.tr03171.ProfileXmlParser
import de.tsenger.vdstools.vds.tr03171.TrVersion
import kotlinx.datetime.LocalDate
import okio.Buffer


/**
 * The registered TR-03171 profile named [name], or null if [name] is a document type (which takes
 * precedence) or no profile. The one rule for building by name, shared by all builders.
 * @throws IllegalArgumentException if several registered profiles have this name
 */
internal fun profileForName(name: String): VdsProfileDefinitionDto? =
    if (DataEncoder.vdsDocumentTypes.getDocumentRef(name) != null) null
    else DataEncoder.vdsProfileDefinitions.resolveByNameUnique(name)

class VdsSeal : Seal {
    private val tag = this::class.simpleName ?: ""

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
            logE(tag, "Signature creation failed: " + e.message)
            return null
        }
    }

    override fun getMessageByName(name: String): Message? {
        val vdsMessage = vdsMessageGroup.getMessageByName(name) ?: return null
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding)
        )
    }

    override fun getMessageByTag(tag: Int): Message? {
        val vdsMessage = vdsMessageGroup.getMessageByTag(tag) ?: return null
        return Message(
            vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
            MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding)
        )
    }

    override val signatureInfo: SignatureInfo?
        get() {
            if (signatureBytes.isEmpty()) return null
            val fieldSize = if (signatureBytes.size % 2 == 0) ecFieldBitSize(signatureBytes.size / 2) else null
            val signatureAlgorithm = when {
                fieldSize == null -> null
                fieldSize <= 224 -> "SHA224_WITH_ECDSA"
                fieldSize <= 256 -> "SHA256_WITH_ECDSA"
                fieldSize <= 384 -> "SHA384_WITH_ECDSA"
                else -> "SHA512_WITH_ECDSA"
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
            Message(
                vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
                MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding)
            )
        }

    override val metadataMessageList: List<Message>
        get() = vdsMessageGroup.metadataMessageList.map { vdsMessage ->
            Message(
                vdsMessage.tag, vdsMessage.name, vdsMessage.coding,
                MessageValue.fromBytes(vdsMessage.value.rawBytes, vdsMessage.coding)
            )
        }

    override val formatFindings: List<FormatFinding>
        get() = if (vdsMessageGroup.vdsType == VdsHeader.UNKNOWN_TYPE) {
            listOf(FormatFinding.UnknownDocumentType(vdsHeader.documentRef))
        } else {
            vdsMessageGroup.formatFindings()
        }

    class Builder private constructor(
        private val headerBuilder: VdsHeader.Builder,
        private val messageBuilder: VdsMessageGroup.Builder
    ) {
        /**
         * Builds a seal for a document type (e.g. "VISA") or a registered TR-03171 profile name. The
         * name is resolved once; a document type takes precedence over a profile of the same name.
         *
         * @throws IllegalArgumentException if several registered profiles have this name (a changed
         *   profile gets a new UUID but may keep its name); use [forProfileUuid] then
         */
        constructor(documentType: String) : this(profileForName(documentType), documentType)

        private constructor(profile: VdsProfileDefinitionDto?, documentType: String) : this(
            profile?.let { VdsHeader.Builder(it) } ?: VdsHeader.Builder(documentType),
            profile?.let { VdsMessageGroup.Builder(it) } ?: VdsMessageGroup.Builder(documentType)
        )

        /**
         * Three-letter code of the issuing state, e.g. "D<<". Defaults to "UTO" (Utopia, the code ICAO
         * Doc 9303-3 recommends for specimen documents), so a seal built without it is a specimen. The
         * document type does not set it.
         */
        fun issuingCountry(v: String) = apply { headerBuilder.setIssuingCountry(v) }
        fun signerIdentifier(v: String) = apply { headerBuilder.setSignerIdentifier(v) }
        fun certificateReference(v: String) = apply { headerBuilder.setCertificateReference(v) }
        fun issuingDate(v: LocalDate) = apply { headerBuilder.setIssuingDate(v) }
        fun sigDate(v: LocalDate) = apply { headerBuilder.setSigDate(v) }
        fun <T> addMessage(tag: Int, value: T) = apply { messageBuilder.addMessage(tag, value) }
        fun <T> addMessage(name: String, value: T) = apply { messageBuilder.addMessage(name, value) }

        fun build(signer: Signer): VdsSeal {
            return VdsSeal(headerBuilder.build(), messageBuilder.build(), signer)
        }

        companion object {
            /**
             * Builds a seal for a BSI TR-03171 document profile identified by its UUID. The profile
             * must be registered beforehand (e.g. via [DataEncoder.loadVdsProfileDefinitionFromXml]).
             * Unlike [Builder] by name, this resolves the profile by its UUID, so several profiles may
             * share the same profile name without ambiguity. A UUID may be registered for both TR-03171
             * v0.8 and v0.9; then [trVersion] selects the profile (and thus document category 0xC8/0xC9).
             *
             * @param profileUuid the profile UUID (32 hex chars, dashes ignored, case-insensitive)
             * @param trVersion the TR-03171 version of the profile; may be omitted if the UUID is
             *   registered for only one version
             * @throws IllegalArgumentException if no profile is registered for the UUID (and version),
             *   or if [trVersion] is omitted and the UUID is registered for several versions
             */
            fun forProfileUuid(profileUuid: String, trVersion: TrVersion? = null): Builder {
                val registry = DataEncoder.vdsProfileDefinitions
                val profileDefinition = (if (trVersion == null) registry.resolveUnique(profileUuid)
                else registry.resolve(profileUuid, trVersion.baseDocumentType))
                    ?: throw IllegalArgumentException(
                        "No TR-03171 profile registered for UUID: $profileUuid" + (trVersion?.let { " ($it)" } ?: "")
                    )
                return forProfile(profileDefinition)
            }

            /**
             * Builds a seal for a TR-03171 document profile the caller supplies, e.g. from its own profile
             * store (create it with [ProfileXmlParser.parse] and [ProfileConverter.toVdsProfileDefinition]).
             * The profile is not registered; parsing such a seal resolves it via [DataEncoder.vdsProfileResolver].
             *
             * @throws IllegalArgumentException if the profile's base document type does not carry a
             *   profile number (`uuidMessageLookup`), e.g. a custom type
             */
            fun forProfile(profileDefinition: VdsProfileDefinitionDto): Builder {
                val base = DataEncoder.vdsDocumentTypes.findDocumentTypeDto(profileDefinition.baseDocumentType)
                require(base?.uuidMessageLookup == true) {
                    "Base document type '${profileDefinition.baseDocumentType}' of profile " +
                            "'${profileDefinition.definitionName}' does not carry a profile number (uuidMessageLookup)"
                }
                return Builder(VdsHeader.Builder(profileDefinition), VdsMessageGroup.Builder(profileDefinition))
            }
        }
    }

    companion object {
        private const val TAG = "VdsSeal"
        internal fun fromRawString(rawString: String): Seal {
            return parseVdsSeal(DataEncoder.decodeBase256(rawString))
        }

        internal fun fromByteArray(rawBytes: ByteArray): Seal {
            return parseVdsSeal(rawBytes)
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun parseVdsSeal(rawBytes: ByteArray): Seal {
            val rawDataBuffer = Buffer().write(rawBytes)
            logV(TAG, "rawData: ${rawBytes.toHexString()}")

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
            // binds document type, profile (via DataEncoder.vdsProfileResolver) and messages
            val vdsMessageGroup = VdsMessageGroup(vdsHeader.vdsType, messageList)
            vdsMessageGroup.profileDefinition?.let { logD(TAG, "Resolved profile definition: ${it.definitionName}") }

            return VdsSeal(vdsHeader, vdsMessageGroup, vdsSignature)
        }
    }
}
