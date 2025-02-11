package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
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

    @Throws(IllegalArgumentException::class)
    fun <T> decodeFeature(vdsType: String, derTlv: DerTlv): T {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return decodeFeature(sealDto, derTlv)
    }


    @Throws(IllegalArgumentException::class)
    fun <T> encodeFeature(vdsType: String, feature: String, inputValue: T): DerTlv {
        if (!vdsTypes.containsKey(vdsType)) {
            log.w("No VdsSeal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        if (!vdsFeatures.contains(feature)) {
            log.w("No VdsSeal feature with name '$feature' was found.")
            throw IllegalArgumentException("No VdsSeal feature with name '$feature' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return encodeFeature(sealDto, feature, inputValue)
    }

    @Throws(IllegalArgumentException::class)
    private fun <T> encodeFeature(sealDto: SealDto, feature: String, inputValue: T): DerTlv {
        val tag = getTag(sealDto, feature)
        if (tag.toInt() == 0) {
            log.w("VdsType: " + sealDto.documentType + " has no Feature " + feature)
            throw IllegalArgumentException("VdsType: " + sealDto.documentType + " has no Feature " + feature)
        }
        val coding = getCoding(sealDto, feature)
        val value: ByteArray
        when (coding) {
            FeatureCoding.C40, FeatureCoding.MRZ -> {
                val valueStr = (inputValue as String).replace("\r".toRegex(), "").replace("\n".toRegex(), "")
                value = DataEncoder.encodeC40(valueStr)
            }

            FeatureCoding.UTF8_STRING -> value = (inputValue as String).encodeToByteArray()
            FeatureCoding.BYTE -> value = byteArrayOf(inputValue as Byte)
            FeatureCoding.BYTES -> value = inputValue as ByteArray
            FeatureCoding.MASKED_DATE -> value = DataEncoder.encodeMaskedDate(inputValue as String)
            FeatureCoding.UNKNOWN -> value = inputValue as ByteArray
        }
        return DerTlv(tag, value)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun <T> decodeFeature(sealDto: SealDto, derTlv: DerTlv): T {
        val tag = derTlv.tag
        val coding = getCoding(sealDto, tag)
        val result = when (coding) {
            FeatureCoding.C40 -> DataEncoder.decodeC40(derTlv.value)
            FeatureCoding.UTF8_STRING -> derTlv.value.decodeToString()
            FeatureCoding.BYTE -> derTlv.value[0]
            FeatureCoding.BYTES -> derTlv.value
            FeatureCoding.MASKED_DATE -> DataEncoder.decodeMaskedDate(derTlv.value)
            FeatureCoding.UNKNOWN -> derTlv.value
            FeatureCoding.MRZ -> {
                val unformattedMrz = DataEncoder.decodeC40(derTlv.value)
                val mrzLength = getFeatureDto(sealDto, tag).decodedLength
                DataEncoder.formatMRZ(unformattedMrz, mrzLength)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    @Throws(IllegalArgumentException::class)
    private fun getTag(sealDto: SealDto, feature: String): Byte {
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

