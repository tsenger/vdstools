package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageResolver
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.tr03171.Tr03171Conformance
import okio.Buffer


internal class VdsMessageGroup {
    internal var derTlvList: List<DerTlv>
    var vdsType: String
        internal set

    /**
     * The resolved extended message definition for UUID-based seals.
     * Null if no definition lookup was performed or no matching definition was found.
     */
    var profileDefinition: VdsProfileDefinitionDto? = null
        internal set

    /**
     * The document profile UUID (Tag 0) for UUID-based seals.
     * Set during [resolveProfileDefinition] and excluded from [messageList].
     */
    var documentProfileUuid: ByteArray? = null
        internal set

    internal val metadataTags: MutableSet<Int> = mutableSetOf()

    constructor(vdsType: String, derTlvList: List<DerTlv>) {
        this.vdsType = vdsType
        this.derTlvList = derTlvList
    }

    private constructor(builder: Builder) {
        this.derTlvList = builder.derTlvList
        this.vdsType = builder.vdsType
    }

    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            for (derTlv in derTlvList) {
                buffer.write(derTlv.encoded)
            }
            return buffer.readByteArray()
        }

    val messageList: List<Message>
        /**
         * @return all messages except metadata tags (e.g. the UUID tag), in seal order, decoded via
         * the resolved profile definition and the base type. A tag that neither defines is returned
         * raw with a name like "0x0A" and coding [MessageCoding.UNKNOWN] (ICAO Doc 9303-13 §2.3:
         * unknown tags may be present).
         */
        get() = derTlvList.filter { it.tag.toInt() !in metadataTags }.map(::resolve)

    /** The metadata tags of [messageList], resolved the same way. */
    val metadataMessageList: List<Message>
        get() = derTlvList.filter { it.tag.toInt() in metadataTags }.map(::resolve)

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
     * Resolves the extended message definition based on the UUID in the specified tag.
     * This method should be called after parsing for seal types that require UUID lookup.
     * The lookup goes through [DataEncoder.vdsProfileResolver], together with the seal's
     * `PROFILE_URI` if the seal type defines one and the base type ([vdsType]).
     *
     * @param uuidTag The tag number containing the UUID (typically 0)
     */
    fun resolveProfileDefinition(uuidTag: Int) {
        val uuidTlv = derTlvList.find { it.tag.toInt() == uuidTag }
        if (uuidTlv != null) {
            documentProfileUuid = uuidTlv.value
            metadataTags.add(uuidTag)
            profileDefinition = DataEncoder.vdsProfileResolver.resolve(uuidTlv.value, profileUri(), vdsType)
        }
    }

    /**
     * Format findings of this message group (see [FormatFinding]). Without a resolved profile the
     * content tags cannot be checked, so they are covered by [FormatFinding.ProfileNotFound] alone.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun formatFindings(): List<FormatFinding> {
        val registry = DataEncoder.vdsDocumentTypes
        val findings = mutableListOf<FormatFinding>()
        val needsProfile = registry.requiresProfileLookup(vdsType)
        if (needsProfile) {
            if (profileDefinition == null) {
                findings += FormatFinding.ProfileNotFound(documentProfileUuid?.toHexString()?.uppercase(), profileUri())
            }
            val uuidTag = registry.getUuidMessageTag(vdsType)
            if (derTlvList.none { it.tag.toInt() == uuidTag }) {
                findings += FormatFinding.MissingRequired(listOf(uuidTag), registry.getMessageName(vdsType, null, uuidTag))
            }
            val tags = derTlvList.map { it.tag.toInt() and 0xFF }.toSet()
            findings += Tr03171Conformance.check(vdsType, tags, profileDefinition)
            profileDefinition?.let { findings += Tr03171Conformance.checkLengths(messageList, it) }
        }
        for (message in metadataMessageList.filter { it.isRaw }) {
            findings += if (message.coding == MessageCoding.UNKNOWN) FormatFinding.ReservedTag(listOf(message.tag))
            else FormatFinding.DecodeFailed(listOf(message.tag), message.name)
        }
        for (message in messageList.filter { it.isRaw }) {
            if (message.coding != MessageCoding.UNKNOWN) {
                findings += FormatFinding.DecodeFailed(listOf(message.tag), message.name)
            } else if (!needsProfile || profileDefinition != null) {
                findings += FormatFinding.UnknownTag(listOf(message.tag))
            }
        }
        return findings
    }

    private fun profileUri(): String? {
        val tag = try {
            DataEncoder.vdsDocumentTypes.getMessageTag(vdsType, "PROFILE_URI")
        } catch (_: IllegalArgumentException) {
            return null
        }
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

        /** Builds a message group for a known base document type or predefined profile name. */
        constructor(vdsType: String) : this() {
            this.vdsType = vdsType
            val docRef = DataEncoder.vdsDocumentTypes.getDocumentRef(vdsType)
            if (docRef == null) {
                // Not a base type — try resolving as a predefined extended definition name
                val extDef = DataEncoder.vdsProfileDefinitions.resolveByNameUnique(vdsType)
                if (extDef != null) {
                    profileDefinition = extDef
                    baseVdsType = extDef.baseDocumentType
                } else {
                    baseVdsType = vdsType
                }
            } else {
                baseVdsType = vdsType
            }
        }

        /** Builds a message group for an already-resolved (e.g. TR-03171) profile definition. */
        constructor(profileDefinition: VdsProfileDefinitionDto) : this() {
            this.vdsType = profileDefinition.definitionName
            this.profileDefinition = profileDefinition
            this.baseVdsType = profileDefinition.baseDocumentType
        }

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
         * Enforces the encoder-side requirements of BSI TR-03171 v0.9 (document category 0xC9):
         * PROFILE_URI (0x03) and CERTIFICATE_URI (0x04) are mandatory, VALID_FROM (0x01) / VALID_TO (0x02)
         * must be present or absent as the profile's validFromPresent / validToPresent demand, and all
         * non-optional profile entries must be present. No-op for all other document types.
         */
        @Throws(IllegalArgumentException::class)
        fun requireTr03171Conformance() {
            if (baseVdsType != DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9) return
            val tags = derTlvList.map { it.tag.toInt() and 0xFF }.toSet()
            val violations = Tr03171Conformance.check(baseVdsType, tags, profileDefinition)
            require(violations.isEmpty()) {
                "TR-03171: " + violations.joinToString("; ", transform = Tr03171Conformance::describe)
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun build(): VdsMessageGroup {
            val group = VdsMessageGroup(this)
            // If using an extended definition, inject UUID as Tag 0 and set definition on group
            val definition = profileDefinition
            if (definition != null) {
                val uuidBytes = definition.definitionId.hexToByteArray()
                val uuidTlv = DerTlv(0.toByte(), uuidBytes)
                group.derTlvList = listOf(uuidTlv) + group.derTlvList
                group.vdsType = baseVdsType
                group.profileDefinition = definition
                group.documentProfileUuid = uuidBytes
                DataEncoder.vdsDocumentTypes.getMetadataTags(baseVdsType).forEach { group.metadataTags.add(it) }
                if (group.metadataTags.isEmpty()) group.metadataTags.add(0)
            }
            return group
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessageGroup {
            val derTlvList = DerTlv.parseAll(rawBytes)
            return VdsMessageGroup(vdsType, derTlvList)
        }
    }
}
