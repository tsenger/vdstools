package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.dto.ExtendedFeatureDefinitionDto
import de.tsenger.vdstools.vds.dto.FeaturesDto
import de.tsenger.vdstools.vds.dto.SealDto
import kotlinx.serialization.json.Json


class FeatureConverter(jsonString: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "")
    private var sealDtoList: List<SealDto>

    private val vdsTypes: MutableMap<String, Int> = HashMap()
    private val vdsTypesReverse: MutableMap<Int, String> = HashMap()
    private val vdsFeatures: MutableSet<String> = mutableSetOf()


    init {
        val json = Json { ignoreUnknownKeys = true }
        this.sealDtoList = json.decodeFromString(jsonString)
        populateMappings()
    }

    private fun populateMappings() {
        for ((documentType, documentRef, _, features) in sealDtoList) {
            if (documentType != "" && documentRef != "") {
                vdsTypes[documentType] = documentRef.toInt(16)
                vdsTypesReverse[documentRef.toInt(16)] = documentType
            }
            features.forEach { vdsFeatures.add(it.name) }
        }
    }

    val availableVdsTypes: List<String>
        get() = vdsTypes.keys.toList()

    fun getDocumentRef(vdsType: String): Int? {
        return vdsTypes[vdsType]
    }

    fun getVdsType(docRef: Int): String? {
        return vdsTypesReverse[docRef]
    }

    /**
     * Checks if the given vdsType requires UUID-based profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return true if this type requires UUID lookup, false otherwise
     */
    fun requiresUuidLookup(vdsType: String): Boolean {
        return try {
            val sealDto = getSealDto(vdsType)
            sealDto.uuidFeatureLookup
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Gets the tag number containing the UUID for profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return The tag number (default 0 if not specified or type not found)
     */
    fun getUuidFeatureTag(vdsType: String): Int {
        return try {
            val sealDto = getSealDto(vdsType)
            sealDto.uuidFeatureTag
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    val availableVdsFeatures: Set<String?>
        get() = vdsFeatures

    @Throws(IllegalArgumentException::class)
    fun getFeatureName(vdsType: String, derTlv: DerTlv): String {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return getFeatureName(sealDto, derTlv.tag.toInt())
    }

    @Throws(IllegalArgumentException::class)
    fun getFeatureCoding(vdsType: String, derTlv: DerTlv): FeatureCoding {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        val tag = derTlv.tag
        return getCoding(sealDto, tag)
    }

    /**
     * Gets the feature name for a given tag, considering extended feature definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended feature definition (may be null)
     * @param tag The tag number to look up
     * @return The feature name
     */
    @Throws(IllegalArgumentException::class)
    fun getFeatureName(baseVdsType: String, extendedDefinition: ExtendedFeatureDefinitionDto?, tag: Int): String {
        // Try extended definition first if available
        if (extendedDefinition != null) {
            val definitionFeature = extendedDefinition.features.find { it.tag == tag }
            if (definitionFeature != null) {
                return definitionFeature.name
            }
        }
        // Fall back to base type
        val sealDto = getSealDto(baseVdsType)
        return getFeatureName(sealDto, tag)
    }

    /**
     * Gets the feature coding for a given tag, considering extended feature definitions.
     * Lookup order: Extended definition first (if provided), then base type.
     *
     * @param baseVdsType The base VDS type (e.g., "ADMINISTRATIVE_DOCUMENTS")
     * @param extendedDefinition The resolved extended feature definition (may be null)
     * @param tag The tag number to look up
     * @return The feature coding
     */
    @Throws(IllegalArgumentException::class)
    fun getFeatureCoding(
        baseVdsType: String,
        extendedDefinition: ExtendedFeatureDefinitionDto?,
        tag: Int
    ): FeatureCoding {
        // Try extended definition first if available
        if (extendedDefinition != null) {
            val definitionFeature = extendedDefinition.features.find { it.tag == tag }
            if (definitionFeature != null) {
                return definitionFeature.coding
            }
        }
        // Fall back to base type
        val sealDto = getSealDto(baseVdsType)
        return getCoding(sealDto, tag.toByte())
    }


    @Throws(IllegalArgumentException::class)
    fun <T> encodeFeature(vdsType: String, featureName: String, inputValue: T): DerTlv {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No VdsSeal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        if (!vdsFeatures.contains(featureName)) {
            log.w("No VdsSeal feature with name '$featureName' was found.")
            throw IllegalArgumentException("No VdsSeal feature with name '$featureName' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return encodeFeature(sealDto, featureName, inputValue)
    }

    @Throws(IllegalArgumentException::class)
    private fun <T> encodeFeature(sealDto: SealDto, featureName: String, inputValue: T): DerTlv {
        val tag = getFeatureTag(sealDto, featureName)
        if (tag.toInt() == 0) {
            log.w("VdsType: " + sealDto.documentType + " has no Feature " + featureName)
            throw IllegalArgumentException("VdsType: " + sealDto.documentType + " has no Feature " + featureName)
        }
        val coding = getCoding(sealDto, featureName)
        val value = DataEncoder.encodeValueByCoding(coding, inputValue)
        return DerTlv(tag, value)
    }


    @Throws(IllegalArgumentException::class)
    fun getFeatureTag(vdsType: String, featureName: String): Int {
        val sealDto = getSealDto(vdsType)
        return getFeatureTag(sealDto, featureName).toInt()
    }

    @Throws(IllegalArgumentException::class)
    fun getFeatureCoding(vdsType: String, tag: Int): FeatureCoding {
        val sealDto = getSealDto(vdsType)
        return getCoding(sealDto, tag.toByte())
    }

    @Throws(IllegalArgumentException::class)
    private fun getFeatureTag(sealDto: SealDto, feature: String): Byte {
        for ((name, tag) in sealDto.features) {
            if (name.equals(feature, ignoreCase = true)) {
                return tag.toByte()
            }
        }
        throw IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getFeatureName(sealDto: SealDto, tag: Int): String {
        for ((name, tag1) in sealDto.features) {
            if (tag1 == tag) {
                return name
            }
        }
        throw IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, feature: String): FeatureCoding {
        for ((name, _, coding) in sealDto.features) {
            if (name.equals(feature, ignoreCase = true)) {
                return coding
            }
        }
        throw IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, tag: Byte): FeatureCoding {
        for ((_, tag1, coding) in sealDto.features) {
            if (tag1 == tag.toInt()) {
                return coding
            }
        }
        throw IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getFeatureDto(sealDto: SealDto, tag: Byte): FeaturesDto {
        for (featureDto in sealDto.features) {
            if (featureDto.tag == tag.toInt()) {
                return featureDto
            }
        }
        throw IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getSealDto(vdsType: String): SealDto {
        for (sealDto in sealDtoList) {
            if (sealDto.documentType == vdsType) {
                return sealDto
            }
        }
        throw IllegalArgumentException("VdsType '$vdsType' is unspecified in SealCodings.")
    }


}

