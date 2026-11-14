package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageResolver
import de.tsenger.vdstools.vds.dto.VdsDocumentTypeDto
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.tr03171.Tr03171Conformance
import okio.Buffer


/**
 * The message zone of a VDS seal. All definitions are bound when the group is created (parsed or
 * built): the document type, the TR-03171 profile and the decoded messages. Definitions loaded or
 * replaced later do not change it; parse the seal again to apply them.
 *
 * @param profile a profile already chosen by the builder; null to look it up via
 *   [DataEncoder.vdsProfileResolver] if the document type requires one
 */
internal class VdsMessageGroup(
    val vdsType: String,
    internal val derTlvList: List<DerTlv>,
    profile: VdsProfileDefinitionDto? = null
) {
    /** The definition of [vdsType] at creation, null for an unknown document type. */
    private val documentTypeDto: VdsDocumentTypeDto? = DataEncoder.vdsDocumentTypes.findDocumentTypeDto(vdsType)

    private val requiresProfile = documentTypeDto?.uuidMessageLookup == true


    /** The document profile UUID (UUID tag, usually 0) of a seal whose type requires a profile, else null. */
    val documentProfileUuid: ByteArray? =
        if (requiresProfile) derTlvList.find { it.tag.toInt() == documentTypeDto!!.uuidMessageTag }?.value else null

    /** The resolved TR-03171 profile; null if the type requires none or it could not be resolved. */
    val profileDefinition: VdsProfileDefinitionDto? =
        profile ?: documentProfileUuid?.let { DataEncoder.vdsProfileResolver.resolve(it, profileUri(), vdsType) }

    /** Tags of the metadata messages (e.g. the profile number), excluded from [messageList]. */
    internal val metadataTags: Set<Int> =
        if (requiresProfile) documentTypeDto!!.metadataTagList.toSet() + documentTypeDto.uuidMessageTag
        else emptySet()

    /**
     * All messages except metadata tags (e.g. the UUID tag), in seal order, decoded via the resolved
     * profile definition and the base type. A tag that neither defines is returned raw with a name
     * like "0x0A" and coding [MessageCoding.UNKNOWN] (ICAO Doc 9303-13 §2.3: unknown tags may be present).
     */
    val messageList: List<Message> = derTlvList.filter { it.tag.toInt() !in metadataTags }.map(::resolve)

    /** The metadata tags of [messageList], resolved the same way. */
    val metadataMessageList: List<Message> = derTlvList.filter { it.tag.toInt() in metadataTags }.map(::resolve)

    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            for (derTlv in derTlvList) {
                buffer.write(derTlv.encoded)
            }
            return buffer.readByteArray()
        }

    private fun resolve(derTlv: DerTlv): Message =
        DataEncoder.vdsDocumentTypes.resolveMessage(vdsType, profileDefinition, derTlv)
            ?: MessageResolver.unresolved(derTlv)

    /** Looks up a message by name, ignoring case (names come verbatim from the definition or profile). */
    fun getMessageByName(messageName: String): Message? {
        return messageList.firstOrNull { message: Message -> message.name.equals(messageName, ignoreCase = true) }
    }

    fun getMessageByTag(messageTag: Int): Message? {
        return messageList.firstOrNull { message: Message -> message.tag == messageTag }
    }

    /**
     * Format findings of this message group (see [FormatFinding]), checked against the definitions
     * bound at creation. Without a resolved profile the content tags cannot be checked, so they are
     * covered by [FormatFinding.ProfileNotFound] alone.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun formatFindings(): List<FormatFinding> {
        val findings = mutableListOf<FormatFinding>()
        val docType = documentTypeDto
        val specifiedType = docType?.takeIf { it.specification != null }
        if (docType != null && requiresProfile) {
            if (profileDefinition == null) {
                findings += FormatFinding.ProfileNotFound(documentProfileUuid?.toHexString()?.uppercase(), profileUri())
            }
            // a verified base type reports a missing profile number via its definition
            val uuidTag = docType.uuidMessageTag
            if (documentProfileUuid == null && specifiedType == null) {
                val name = docType.messages.firstOrNull { it.tag == uuidTag }?.name
                    ?: ("0x" + uuidTag.toString(16).uppercase().padStart(2, '0'))
                findings += FormatFinding.MissingRequired(listOf(uuidTag), name)
            }
        }
        if (specifiedType != null) findings += specificationFindings(specifiedType)
        if (docType != null && requiresProfile) {
            val tags = derTlvList.map { it.tag.toInt() and 0xFF }.toSet()
            findings += Tr03171Conformance.check(vdsType, tags, profileDefinition)
            profileDefinition?.let { findings += Tr03171Conformance.checkLengths(messageList, it) }
        }
        for (message in metadataMessageList.filter { it.isRaw }) {
            findings += if (message.isDefined) FormatFinding.DecodeFailed(listOf(message.tag), message.name)
            else FormatFinding.ReservedTag(listOf(message.tag))
        }
        for (message in messageList.filter { it.isRaw }) {
            if (message.isDefined) {
                findings += FormatFinding.DecodeFailed(listOf(message.tag), message.name)
            } else if (specifiedType?.additionalFeaturesAllowed == false) {
                findings += FormatFinding.NotAllowed(listOf(message.tag), message.name)
            } else if (!requiresProfile || profileDefinition != null) {
                findings += FormatFinding.UnknownTag(listOf(message.tag))
            }
        }
        return findings
    }

    /**
     * Required messages and lengths of a document type whose definitions are verified against its
     * specification ([VdsDocumentTypeDto.specification]). Lengths are bytes; a `maxBytes` of 0 means
     * the specification gives no maximum. Metadata messages (e.g. TR-03171 v0.9 validity dates) are
     * included; raw messages are skipped.
     */
    private fun specificationFindings(docType: VdsDocumentTypeDto): List<FormatFinding> {
        val tags = derTlvList.map { it.tag.toInt() and 0xFF }.toSet()
        val findings = mutableListOf<FormatFinding>()
        docType.messages.filter { it.required && it.tag !in tags }
            .forEach { findings += FormatFinding.MissingRequired(listOf(it.tag), it.name) }
        for (group in docType.requiredOneOf.filter { group -> group.none { it in tags } }) {
            val names = group.map { tag -> docType.messages.firstOrNull { it.tag == tag }?.name ?: "$tag" }
            findings += FormatFinding.MissingRequired(emptyList(), names.joinToString(" or "))
        }
        for (message in (metadataMessageList + messageList).filter { !it.isRaw }) {
            val definition = docType.messages.firstOrNull { it.tag == message.tag } ?: continue
            val length = message.value.rawBytes.size
            val maxBytes = definition.maxBytes.takeIf { it > 0 }
            if (length < definition.minBytes || (maxBytes != null && length > maxBytes)) {
                findings += FormatFinding.InvalidLength(listOf(message.tag), message.name, length, definition.minBytes, maxBytes)
            }
        }
        return findings
    }

    /** The seal's PROFILE_URI if its document type defines one. */
    private fun profileUri(): String? {
        val tag = documentTypeDto?.messages?.firstOrNull { it.name == "PROFILE_URI" }?.tag ?: return null
        return derTlvList.find { it.tag.toInt() == tag }?.value?.decodeToString()
    }

    internal class Builder private constructor() {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)
        var vdsType: String = ""
            private set
        internal var baseVdsType: String = ""
            private set
        internal var profileDefinition: VdsProfileDefinitionDto? = null
            private set

        /** Builds a message group for a document type or registered profile name (see [profileForName]). */
        constructor(vdsType: String) : this(vdsType, profileForName(vdsType))

        /** Builds a message group for an already-resolved (e.g. TR-03171) profile definition. */
        constructor(profileDefinition: VdsProfileDefinitionDto) : this(profileDefinition.definitionName, profileDefinition)

        /**
         * Builds a message group for [vdsType], or with an already-resolved [profile] for that profile.
         *
         * @throws IllegalArgumentException if the profile's base document type does not carry a profile
         *   number (`uuidMessageLookup`), e.g. a custom type
         */
        constructor(vdsType: String, profile: VdsProfileDefinitionDto?) : this() {
            this.profileDefinition = profile
            if (profile == null) {
                this.vdsType = vdsType
                this.baseVdsType = vdsType
            } else {
                val base = DataEncoder.vdsDocumentTypes.findDocumentTypeDto(profile.baseDocumentType)
                require(base?.uuidMessageLookup == true) {
                    "Base document type '${profile.baseDocumentType}' of profile '${profile.definitionName}' " +
                            "does not carry a profile number (uuidMessageLookup)"
                }
                this.vdsType = profile.definitionName
                this.baseVdsType = profile.baseDocumentType
                this.uuidMessageTag = base.uuidMessageTag
            }
        }

        /** The tag of the profile number in the base document type (TR-03171: 0). */
        private var uuidMessageTag: Int = 0

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(tag: Int, value: T): Builder {
            val coding = DataEncoder.vdsDocumentTypes.getMessageCoding(baseVdsType, profileDefinition, tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addMessage(name: String, value: T): Builder {
            return addMessage(DataEncoder.vdsDocumentTypes.getMessageTag(baseVdsType, profileDefinition, name), value)
        }

        /**
         * Builds the group and binds its definitions. With a profile, its UUID is prepended as the
         * profile number (the base type's UUID tag, TR-03171: 0) and the group is typed as the
         * profile's base document type.
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun build(): VdsMessageGroup {
            val definition = profileDefinition ?: return VdsMessageGroup(vdsType, derTlvList.toList())
            val uuidTlv = DerTlv(uuidMessageTag.toByte(), definition.definitionId.hexToByteArray())
            return VdsMessageGroup(baseVdsType, listOf(uuidTlv) + derTlvList, definition)
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessageGroup {
            val derTlvList = DerTlv.parseAll(rawBytes)
            return VdsMessageGroup(vdsType, derTlvList)
        }
    }
}
