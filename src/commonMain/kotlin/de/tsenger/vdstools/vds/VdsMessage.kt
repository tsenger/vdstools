package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.dto.ExtendedFeatureDefinitionDto
import okio.Buffer


class VdsMessage {
    private var derTlvList: List<DerTlv>
    var vdsType: String
        private set

    /**
     * The resolved extended feature definition for UUID-based seals.
     * Null if no definition lookup was performed or no matching definition was found.
     */
    var extendedFeatureDefinition: ExtendedFeatureDefinitionDto? = null
        private set

    /**
     * The effective VDS type, considering extended feature definition resolution.
     * Returns the definition name if resolved, otherwise the base vdsType.
     */
    val effectiveVdsType: String
        get() = extendedFeatureDefinition?.definitionName ?: vdsType

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
            for (feature in derTlvList) {
                buffer.write(feature.encoded)
            }
            return buffer.readByteArray()
        }

    val featureList: List<Feature>
        /**
         * @return a list of all decoded Features, using extended feature definition-aware lookup if available
         */
        get() {
            val featureList: MutableList<Feature> = ArrayList()
            for (derTlv in derTlvList) {
                DataEncoder.encodeDerTlv(vdsType, extendedFeatureDefinition, derTlv)?.let { featureList.add(it) }
            }
            return featureList
        }

    fun getFeature(featureName: String): Feature? {
        return featureList.firstOrNull { feature: Feature -> feature.name == featureName }
    }

    fun getFeature(featureTag: Int): Feature? {
        return featureList.firstOrNull { feature: Feature -> feature.tag == featureTag }
    }

    /**
     * Resolves the extended feature definition based on the UUID in the specified tag.
     * This method should be called after parsing for seal types that require UUID lookup.
     *
     * @param uuidTag The tag number containing the UUID (typically 0)
     * @return The definition name if resolved, or the base vdsType if no definition found
     */
    fun resolveExtendedFeatureDefinition(uuidTag: Int): String {
        val uuidTlv = derTlvList.find { it.tag.toInt() == uuidTag }
        if (uuidTlv != null) {
            extendedFeatureDefinition = DataEncoder.resolveExtendedFeatureDefinition(uuidTlv.value)
        }
        return effectiveVdsType
    }

    class Builder(val vdsType: String) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        @Throws(IllegalArgumentException::class)
        fun <T> addDocumentFeature(feature: String, value: T): Builder {
            val derTlv = DataEncoder.encodeFeature(this.vdsType, feature, value)
            derTlvList.add(derTlv)
            return this
        }

        fun build(): VdsMessage {
            return VdsMessage(this)
        }
    }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, vdsType: String): VdsMessage {
            val derTlvList = DataEncoder.parseDerTLvs(rawBytes)
            return VdsMessage(vdsType, derTlvList)
        }
    }
}
