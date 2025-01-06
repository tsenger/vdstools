package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import de.tsenger.vdstools.DataEncoder.encodeC40
import de.tsenger.vdstools.DataParser.decodeC40
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureCoding
import de.tsenger.vdstools.vds.dto.FeaturesDto
import de.tsenger.vdstools.vds.dto.SealDto

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.*

class FeatureConverter @JvmOverloads constructor(`is`: InputStream? = null) {
    private val sealDtoList: List<SealDto>

    init {
        var `is` = `is`
        val gson = GsonBuilder()
            .registerTypeAdapter(FeatureCoding::class.java, FeatureEncodingDeserializer())
            .create()
        // Definiere den Typ für die Liste von Document-Objekten
        val listType = object : TypeToken<List<SealDto?>?>() {
        }.type

        if (`is` == null) {
            `is` = javaClass.getResourceAsStream(DEFAULT_SEAL_CODINGS)
        }
        val reader = BufferedReader(InputStreamReader(`is`))
        this.sealDtoList = gson.fromJson(reader, listType)

        for ((documentType, documentRef, _, features) in sealDtoList) {
            vdsTypes[documentType] = documentRef!!.toInt(16)
            vdsTypesReverse[documentRef.toInt(16)] = documentType
            for ((name) in features!!) {
                vdsFeatures.add(name)
            }
        }
    }

    val availableVdsTypes: Set<String?>
        get() = TreeSet(vdsTypes.keys)

    fun getDocumentRef(vdsType: String): Int {
        requireNotNull(vdsTypes[vdsType]) { "Could find seal type " + vdsType + " in " + DEFAULT_SEAL_CODINGS }
        return vdsTypes[vdsType]!!
    }

    fun getVdsType(docRef: Int): String? {
        return vdsTypesReverse[docRef]
    }

    val availableVdsFeatures: Set<String?>
        get() = vdsFeatures

    @Throws(IllegalArgumentException::class)
    fun getFeatureName(vdsType: String, derTlv: DerTlv): String? {
        if (!vdsTypes.containsKey(vdsType)) {
            Logger.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return getFeatureName(sealDto, derTlv.tag.toInt())
    }

    @Throws(IllegalArgumentException::class)
    fun getFeatureCoding(vdsType: String, derTlv: DerTlv): FeatureCoding? {
        if (!vdsTypes.containsKey(vdsType)) {
            Logger.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        val tag = derTlv.tag
        return getCoding(sealDto, tag)
    }

    @Throws(IllegalArgumentException::class)
    fun <T> decodeFeature(vdsType: String, derTlv: DerTlv): T {
        if (!vdsTypes.containsKey(vdsType)) {
            Logger.w("No seal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return decodeFeature(sealDto, derTlv)
    }


    @Throws(IllegalArgumentException::class)
    fun <T> encodeFeature(vdsType: String, feature: String, inputValue: T): DerTlv {
        if (!vdsTypes.containsKey(vdsType)) {
            Logger.w("No VdsSeal type with name '$vdsType' was found.")
            throw IllegalArgumentException("No seal type with name '$vdsType' was found.")
        }
        if (!vdsFeatures.contains(feature)) {
            Logger.w("No VdsSeal feature with name '$feature' was found.")
            throw IllegalArgumentException("No VdsSeal feature with name '$feature' was found.")
        }
        val sealDto = getSealDto(vdsType)
        return encodeFeature(sealDto, feature, inputValue)
    }

    @Throws(IllegalArgumentException::class)
    private fun <T> encodeFeature(sealDto: SealDto, feature: String, inputValue: T): DerTlv {
        val tag = getTag(sealDto, feature)
        if (tag.toInt() == 0) {
            Logger.w("VdsType: " + sealDto.documentType + " has no Feature " + feature)
            throw IllegalArgumentException("VdsType: " + sealDto.documentType + " has no Feature " + feature)
        }
        val coding = getCoding(sealDto, feature)
        val value: ByteArray
        when (coding) {
            FeatureCoding.C40 -> {
                val valueStr = (inputValue as String).replace("\r".toRegex(), "").replace("\n".toRegex(), "")
                value = encodeC40(valueStr)
            }

            FeatureCoding.UTF8_STRING -> value = (inputValue as String).toByteArray(StandardCharsets.UTF_8)
            FeatureCoding.BYTE -> value = byteArrayOf(inputValue as Byte)
            FeatureCoding.BYTES -> value = inputValue as ByteArray
            else -> value = inputValue as ByteArray
        }
        return DerTlv(tag, value)
    }

    private fun <T> decodeFeature(sealDto: SealDto, derTlv: DerTlv): T {
        val tag = derTlv.tag
        val coding = getCoding(sealDto, tag)
        when (coding) {
            FeatureCoding.C40 -> {
                var featureValue = decodeC40(derTlv.value)
                val featureName = getFeatureName(sealDto, tag.toInt())
                if (featureName != null && featureName.startsWith("MRZ")) {
                    val mrzLength = getFeatureDto(sealDto, tag).decodedLength
                    val newMrz = String.format("%1$-" + mrzLength + "s", featureValue).replace(' ', '<')
                    featureValue = """
                        ${newMrz.substring(0, mrzLength / 2)}
                        ${newMrz.substring(mrzLength / 2)}
                        """.trimIndent()
                }
                return featureValue as T
            }

            FeatureCoding.UTF8_STRING -> return String(derTlv.value, StandardCharsets.UTF_8) as T
            FeatureCoding.BYTE -> return derTlv.value[0] as T
            FeatureCoding.BYTES -> return derTlv.value as T
            else -> return derTlv.value as T
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun getTag(sealDto: SealDto, feature: String): Byte {
        for ((name, tag) in sealDto.features!!) {
            if (name.equals(feature, ignoreCase = true)) {
                return tag.toByte()
            }
        }
        throw IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getFeatureName(sealDto: SealDto, tag: Int): String? {
        for ((name, tag1) in sealDto.features!!) {
            if (tag1 == tag) {
                return name
            }
        }
        throw IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, feature: String): FeatureCoding? {
        for ((name, _, coding) in sealDto.features!!) {
            if (name.equals(feature, ignoreCase = true)) {
                return coding
            }
        }
        throw IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getCoding(sealDto: SealDto, tag: Byte): FeatureCoding? {
        for ((_, tag1, coding) in sealDto.features!!) {
            if (tag1 == tag.toInt()) {
                return coding
            }
        }
        throw IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType + "'")
    }

    @Throws(IllegalArgumentException::class)
    private fun getFeatureDto(sealDto: SealDto, tag: Byte): FeaturesDto {
        for (featureDto in sealDto.features!!) {
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

    companion object {
        private val vdsTypes: MutableMap<String?, Int> = HashMap()
        private val vdsTypesReverse: MutableMap<Int, String?> = HashMap()
        private val vdsFeatures: MutableSet<String?> = TreeSet()
        var DEFAULT_SEAL_CODINGS: String = "/SealCodings.json"
    }
}

internal class FeatureEncodingDeserializer : JsonDeserializer<FeatureCoding> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FeatureCoding {
        val value = json.asString
        try {
            return FeatureCoding.valueOf(value.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            throw JsonParseException("Invalid value for FeatureCoding: $value")
        }
    }
}
