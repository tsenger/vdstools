package de.tsenger.vdstools

import de.tsenger.vdstools.internal.logD
import de.tsenger.vdstools.internal.logW
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import kotlinx.serialization.json.Json

/**
 * Registry for extended message definitions based on UUID lookup.
 *
 * This class enables a two-stage lookup process for seals with UUID-based definitions
 * as specified in TR-03171:
 * 1. The documentRef in the header determines the base type (e.g., ADMINISTRATIVE_DOCUMENTS)
 * 2. The UUID from Tag 0 (Dokumentenprofilnummer) is used to look up the specific definition
 *
 * This extends the limited 256-value space of Document Feature Definition Reference in the header
 * by using UUIDs in the message zone for dynamic definition registration.
 *
 * @param jsonString JSON content containing extended message definitions
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsProfileDefinitionRegistry(jsonString: String) : DefinitionRegistry {
    private val tag = this::class.simpleName ?: ""
    private var definitionsByUuid: Map<String, VdsProfileDefinitionDto>
    private var definitionsByName: Map<String, VdsProfileDefinitionDto>

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    init {
        val definitionList: List<VdsProfileDefinitionDto> = json.decodeFromString(jsonString)
        definitionsByUuid = definitionList.associateBy { it.definitionId.lowercase() }
        definitionsByName = definitionList.associateBy { it.definitionName }
        logD(tag,"Loaded ${definitionsByUuid.size} VDS profile definitions")
    }

    override fun addEntriesFromJson(jsonString: String) {
        val newDtos = json.decodeFromString<List<VdsProfileDefinitionDto>>(jsonString)
        newDtos.forEach { addDefinition(it) }
    }

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
        definitionsByUuid = definitionsByUuid + (definition.definitionId.lowercase() to definition)
        definitionsByName = definitionsByName + (definition.definitionName to definition)
        logD(tag,"Added definition: ${definition.definitionName} (${definition.definitionId})")
    }

    /**
     * Resolves an extended message definition by its definition name.
     *
     * @param definitionName The definition name (e.g., "MELDEBESCHEINIGUNG")
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
