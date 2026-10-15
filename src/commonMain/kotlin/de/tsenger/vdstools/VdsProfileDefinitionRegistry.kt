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
 * 2. The UUID from Tag 0 (Dokumentenprofilnummer) is used to look up the specific definition
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
    private var definitionsByUuid: Map<String, VdsProfileDefinitionDto> = emptyMap()
    private var definitionsByName: Map<String, VdsProfileDefinitionDto> = emptyMap()

    /**
     * Resolves an extended message definition based on the UUID bytes.
     *
     * @param uuidBytes 16-byte UUID from Tag 0
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidBytes: ByteArray): VdsProfileDefinitionDto? {
        if (uuidBytes.size != 16) {
            logW(tag,"Invalid UUID length: expected 16 bytes, got ${uuidBytes.size}")
            return null
        }
        val uuidHex = uuidBytes.toHexString().lowercase()
        val definition = definitionsByUuid[uuidHex]
        if (definition != null) {
            logD(tag,"Resolved definition: ${definition.definitionName} for UUID: $uuidHex")
        } else {
            logD(tag,"No definition found for UUID: $uuidHex")
        }
        return definition
    }

    /**
     * Resolves an extended message definition based on the UUID hex string.
     *
     * @param uuidHex UUID as hex string (32 characters, without dashes)
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidHex: String): VdsProfileDefinitionDto? {
        val normalizedUuid = uuidHex.lowercase().replace("-", "")
        if (normalizedUuid.length != 32) {
            logW(tag,"Invalid UUID hex length: expected 32 characters, got ${normalizedUuid.length}")
            return null
        }
        val definition = definitionsByUuid[normalizedUuid]
        if (definition != null) {
            logD(tag,"Resolved definition: ${definition.definitionName} for UUID: $normalizedUuid")
        } else {
            logD(tag,"No definition found for UUID: $normalizedUuid")
        }
        return definition
    }

    /**
     * Gets all available definition names.
     *
     * @return List of all registered definition names
     */
    /**
     * Adds or replaces an extended message definition in the registry.
     *
     * @param definition The definition to add
     */
    fun addDefinition(definition: VdsProfileDefinitionDto) {
        val uuid = definition.definitionId.lowercase()
        val replaced = definitionsByUuid[uuid]
        if (replaced != null) {
            logW(tag, "Replacing definition ${replaced.definitionName} for UUID: $uuid")
            // drop the old name so it no longer resolves to the replaced definition
            definitionsByName = definitionsByName - replaced.definitionName
        }
        definitionsByUuid = definitionsByUuid + (uuid to definition)
        definitionsByName = definitionsByName + (definition.definitionName to definition)
        logD(tag,"Added definition: ${definition.definitionName} (${definition.definitionId})")
    }

    /**
     * Resolves an extended message definition by its definition name.
     *
     * @param definitionName The definition name (e.g., "Meldebestätigung")
     * @return The matching VdsProfileDefinitionDto, or null if no definition matches
     */
    fun resolveByName(definitionName: String): VdsProfileDefinitionDto? {
        return definitionsByName[definitionName]
    }

    val availableDefinitions: List<String>
        get() = definitionsByUuid.values.map { it.definitionName }

    /**
     * Gets all available definition UUIDs.
     *
     * @return List of all registered definition UUIDs (as lowercase hex strings)
     */
    val availableDefinitionUuids: List<String>
        get() = definitionsByUuid.keys.toList()
}
