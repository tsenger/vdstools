package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import okio.Buffer


class VdsMessage {
    private var derTlvList: List<DerTlv>
    var vdsType: String
        private set

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
         * @return  a list of all decoded Features
         */
        get() {
            val featureList: MutableList<Feature> =
                ArrayList()
            for (derTlv in derTlvList) {
                val feature = DataEncoder.encodeDerTlv(vdsType, derTlv)
                if (feature != null) featureList.add(feature)
            }
            return featureList
        }

    fun getFeature(featureName: String): Feature? {
        return featureList.firstOrNull { feature: Feature -> feature.name == featureName }
    }

    fun getFeature(featureTag: Int): Feature? {
        return featureList.firstOrNull { feature: Feature -> feature.tag == featureTag }
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
