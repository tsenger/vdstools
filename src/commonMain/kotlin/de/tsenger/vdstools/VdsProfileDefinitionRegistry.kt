package de.tsenger.vdstools

import de.tsenger.vdstools.internal.logD
import de.tsenger.vdstools.internal.logW
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto

/**
 * Registry for extended message definitions based on UUID lookup.
 *
 * This class enables a two-stage lookup process for seals with UUID-based definitions
 * as specified in TR-03171:
 * 1. The documentRef in the header determines the base type (e.g., ADMINISTRATIVE_DOCUMENTS_V8)
 * 2. The UUID from Tag 0 (Dokumentenprofilnummer) together with that base type is used to look up
 *    the specific definition. The same UUID may be registered once per base type (TR-03171 v0.8
 *    and v0.9 profiles sharing a UUID are not interchangeable).
 *
 * This extends the limited 256-value space of Document Feature Definition Reference in the header
 * by using UUIDs in the message zone for dynamic definition registration.
 *
 * The registry starts empty; definitions are added from TR-03171 XML document profiles via
 * [DataEncoder.loadVdsProfileDefinitionFromXml].
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsProfileDefinitionRegistry {
    private val tag = this::class.simpleName ?: ""
    // keyed by (UUID, baseDocumentType): a profile UUID may be registered once per TR-03171 version
    private var definitionsByKey: Map<Pair<String, String>, VdsProfileDefinitionDto> = emptyMap()
    // keyed by (definitionName, baseDocumentType)
    private var definitionsByName: Map<Pair<String, String>, VdsProfileDefinitionDto> = emptyMap()

    /**
     * Resolves a profile definition by UUID bytes and base document type.
     *
     * @param uuidBytes 16-byte UUID from Tag 0
     * @param baseDocumentType e.g. [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] or [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9]
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidBytes: ByteArray, baseDocumentType: String): VdsProfileDefinitionDto? {
        if (uuidBytes.size != 16) {
            logW(tag,"Invalid UUID length: expected 16 bytes, got ${uuidBytes.size}")
            return null
        }
        return lookup(uuidBytes.toHexString().lowercase(), baseDocumentType)
    }

    /**
     * Resolves a profile definition by UUID hex string and base document type.
     *
     * @param uuidHex UUID as hex string (32 characters, dashes ignored, case-insensitive)
     * @param baseDocumentType e.g. [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] or [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9]
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidHex: String, baseDocumentType: String): VdsProfileDefinitionDto? {
        val normalizedUuid = normalizeUuid(uuidHex) ?: return null
        return lookup(normalizedUuid, baseDocumentType)
    }

    private fun lookup(uuid: String, baseDocumentType: String): VdsProfileDefinitionDto? {
        val definition = definitionsByKey[uuid to baseDocumentType]
        if (definition != null) {
            logD(tag,"Resolved definition: ${definition.definitionName} for UUID: $uuid ($baseDocumentType)")
        } else {
            logD(tag,"No definition found for UUID: $uuid ($baseDocumentType)")
        }
        return definition
    }

    private fun normalizeUuid(uuidHex: String): String? {
        val normalizedUuid = uuidHex.lowercase().replace("-", "")
        if (normalizedUuid.length != 32) {
            logW(tag,"Invalid UUID hex length: expected 32 characters, got ${normalizedUuid.length}")
            return null
        }
        return normalizedUuid
    }

    /**
     * Adds or replaces a profile definition. A definition with the same UUID and base document type
     * is replaced; the same UUID with another base document type is kept as a separate entry.
     *
     * @param definition The definition to add
     */
    fun addDefinition(definition: VdsProfileDefinitionDto) {
        val key = definition.definitionId.lowercase() to definition.baseDocumentType
        val replaced = definitionsByKey[key]
        if (replaced != null) {
            logW(tag, "Replacing definition ${replaced.definitionName} for UUID: ${key.first} (${key.second})")
            // drop the old name so it no longer resolves to the replaced definition
            val replacedNameKey = replaced.definitionName to replaced.baseDocumentType
            if (definitionsByName[replacedNameKey] == replaced) definitionsByName = definitionsByName - replacedNameKey
        }
        definitionsByKey = definitionsByKey + (key to definition)
        definitionsByName = definitionsByName + ((definition.definitionName to definition.baseDocumentType) to definition)
        logD(tag,"Added definition: ${definition.definitionName} (${definition.definitionId}, ${definition.baseDocumentType})")
    }

    /**
     * Resolves a profile definition by its definition name within a base document type. If several
     * profiles of that base type share the name, the last registered one wins.
     *
     * @param definitionName The definition name (e.g., "Meldebestätigung")
     * @param baseDocumentType e.g. [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] or [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9]
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolveByName(definitionName: String, baseDocumentType: String): VdsProfileDefinitionDto? {
        return definitionsByName[definitionName to baseDocumentType]
    }

    /**
     * Resolves a profile by UUID regardless of base document type.
     * @throws IllegalArgumentException if the UUID is registered for more than one base document type
     */
    internal fun resolveUnique(uuidHex: String): VdsProfileDefinitionDto? {
        val normalizedUuid = normalizeUuid(uuidHex) ?: return null
        return unique(definitionsByKey.filterKeys { it.first == normalizedUuid }.values, "UUID $normalizedUuid")
    }

    /**
     * Resolves a profile by name regardless of base document type.
     * @throws IllegalArgumentException if the name is registered for more than one base document type
     */
    internal fun resolveByNameUnique(definitionName: String): VdsProfileDefinitionDto? =
        unique(definitionsByName.filterKeys { it.first == definitionName }.values, "'$definitionName'")

    private fun unique(matches: Collection<VdsProfileDefinitionDto>, what: String): VdsProfileDefinitionDto? {
        require(matches.size <= 1) {
            "TR-03171 profile $what is registered for several base document types " +
                "(${matches.joinToString { it.baseDocumentType }}); use VdsSeal.Builder.forProfileUuid(uuid, trVersion)"
        }
        return matches.firstOrNull()
    }

    /** All registered profile definitions; each carries its UUID, name and base document type. */
    val definitions: List<VdsProfileDefinitionDto>
        get() = definitionsByKey.values.toList()
}
