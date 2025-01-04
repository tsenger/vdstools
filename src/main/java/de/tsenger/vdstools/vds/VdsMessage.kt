package de.tsenger.vdstools.vds

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.DataParser
import de.tsenger.vdstools.DerTlv
import java.io.ByteArrayOutputStream
import java.io.IOException


class VdsMessage {
    private var derTlvList: List<DerTlv>? = null
    var vdsType: String? = null
        private set

    private constructor()

    constructor(vdsType: String?, derTlvList: List<DerTlv>?) : this() {
        this.vdsType = vdsType
        this.derTlvList = derTlvList
    }

    private constructor(builder: Builder) {
        this.derTlvList = builder.derTlvList
        this.vdsType = builder.vdsType
    }

    val encoded: ByteArray
        get() {
            val baos = ByteArrayOutputStream()
            try {
                for (feature in derTlvList!!) {
                    baos.write(feature.encoded)
                }
            } catch (e: IOException) {
                Logger.e("Can't build raw bytes: " + e.message)
                return ByteArray(0)
            }
            return baos.toByteArray()
        }

    val featureList: List<Feature>
        /**
         * @return  a list of all decoded Features
         */
        get() {
            val featureList: MutableList<Feature> =
                ArrayList()
            for (derTlv in derTlvList!!) {
                featureList.add(DataEncoder.encodeDerTlv(vdsType, derTlv))
            }
            return featureList
        }

    fun getFeature(featureName: String): Feature? {
        return featureList.firstOrNull() { feature: Feature -> feature.name() == featureName }
    }

    class Builder(val vdsType: String) {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        @Throws(IllegalArgumentException::class)
        fun <T> addDocumentFeature(feature: String?, value: T): Builder {
            val derTlv = DataEncoder.encodeFeature(this.vdsType, feature, value)
            derTlvList.add(derTlv)
            return this
        }

        fun build(): VdsMessage {
            return VdsMessage(this)
        }
    }

    companion object {
        @JvmStatic
		fun fromByteArray(rawBytes: ByteArray?, vdsType: String?): VdsMessage {
            val derTlvList = DataParser.parseDerTLvs(rawBytes)
            return VdsMessage(vdsType, derTlvList)
        }
    }
}
