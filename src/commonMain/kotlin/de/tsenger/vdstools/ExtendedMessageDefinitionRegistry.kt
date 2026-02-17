package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
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
class ExtendedMessageDefinitionRegistry(jsonString: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var definitionsByUuid: Map<String, ExtendedMessageDefinitionDto>
    private var definitionsByName: Map<String, ExtendedMessageDefinitionDto>

    init {
        val json = Json { ignoreUnknownKeys = true }
        val definitionList: List<ExtendedMessageDefinitionDto> = json.decodeFromString(jsonString)
        definitionsByUuid = definitionList.associateBy { it.definitionId.lowercase() }
        definitionsByName = definitionList.associateBy { it.definitionName }
        log.d("Loaded ${definitionsByUuid.size} extended message definitions")
    }

    /**
     * Resolves an extended message definition based on the UUID bytes.
     *
     * @param uuidBytes 16-byte UUID from Tag 0
     * @return The matching ExtendedMessageDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidBytes: ByteArray): ExtendedMessageDefinitionDto? {
        if (uuidBytes.size != 16) {
            log.w("Invalid UUID length: expected 16 bytes, got ${uuidBytes.size}")
            return null
        }
        val uuidHex = uuidBytes.toHexString().lowercase()
        val definition = definitionsByUuid[uuidHex]
        if (definition != null) {
            log.d("Resolved definition: ${definition.definitionName} for UUID: $uuidHex")
        } else {
            log.d("No definition found for UUID: $uuidHex")
        }
        return definition
    }

    /**
     * Resolves an extended message definition based on the UUID hex string.
     *
     * @param uuidHex UUID as hex string (32 characters, without dashes)
     * @return The matching ExtendedMessageDefinitionDto, or null if no definition matches
     */
    fun resolve(uuidHex: String): ExtendedMessageDefinitionDto? {
        val normalizedUuid = uuidHex.lowercase().replace("-", "")
        if (normalizedUuid.length != 32) {
            log.w("Invalid UUID hex length: expected 32 characters, got ${normalizedUuid.length}")
            return null
        }
        val definition = definitionsByUuid[normalizedUuid]
        if (definition != null) {
            log.d("Resolved definition: ${definition.definitionName} for UUID: $normalizedUuid")
        } else {
            log.d("No definition found for UUID: $normalizedUuid")
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
    fun addDefinition(definition: ExtendedMessageDefinitionDto) {
        definitionsByUuid = definitionsByUuid + (definition.definitionId.lowercase() to definition)
        definitionsByName = definitionsByName + (definition.definitionName to definition)
        log.d("Added definition: ${definition.definitionName} (${definition.definitionId})")
    }

    /**
     * Resolves an extended message definition by its definition name.
     *
     * @param definitionName The definition name (e.g., "MELDEBESCHEINIGUNG")
     * @return The matching ExtendedMessageDefinitionDto, or null if no definition matches
     */
    fun resolveByName(definitionName: String): ExtendedMessageDefinitionDto? {
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
