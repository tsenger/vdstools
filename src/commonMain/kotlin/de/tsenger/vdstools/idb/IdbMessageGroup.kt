package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.FeatureValue
import okio.Buffer

class IdbMessageGroup {
    private var derTlvList: List<DerTlv>

    constructor(derTlvList: List<DerTlv>) {
        this.derTlvList = derTlvList
    }

    private constructor(builder: Builder) {
        this.derTlvList = builder.derTlvList
    }

    val featureList: List<IdbFeature>
        get() = derTlvList.map { derTlv ->
            val tag = derTlv.tag.toInt() and 0xFF
            val name = DataEncoder.getIdbMessageTypeName(tag)
            val coding = DataEncoder.getIdbMessageTypeCoding(name)
            val value = FeatureValue.fromBytes(derTlv.value, coding)
            IdbFeature(tag, name, coding, value)
        }

    fun getFeature(featureTag: Int): IdbFeature? {
        return featureList.firstOrNull { it.tag == featureTag }
    }

    fun getFeature(featureName: String): IdbFeature? {
        return featureList.firstOrNull { it.name == featureName }
    }

    val encoded: ByteArray
        get() {
            val messages = Buffer()
            for (derTlv in derTlvList) {
                messages.write(derTlv.encoded)
            }
            return DerTlv(TAG, messages.readByteArray()).encoded
        }

    class Builder {
        val derTlvList: MutableList<DerTlv> = ArrayList(5)

        @Throws(IllegalArgumentException::class)
        fun <T> addFeature(tag: Int, value: T): Builder {
            val coding = DataEncoder.getIdbMessageTypeCoding(tag)
            val content = DataEncoder.encodeValueByCoding(coding, value, tag)
            derTlvList.add(DerTlv(tag.toByte(), content))
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun <T> addFeature(name: String, value: T): Builder {
            return addFeature(DataEncoder.getIdbMessageTypeTag(name) ?: 0, value)
        }

        fun build(): IdbMessageGroup {
            return IdbMessageGroup(this)
        }
    }

    companion object {
        const val TAG: Byte = 0x61

        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray): IdbMessageGroup {
            require(rawBytes[0] == TAG) {
                "IdbMessageGroup shall have tag ${
                    TAG.toString(16).padStart(2, '0').uppercase()
                }, but tag ${rawBytes[0].toString(16).padStart(2, '0').uppercase()} was found instead."
            }
            val valueBytes = DerTlv.fromByteArray(rawBytes)?.value ?: ByteArray(0)
            val derTlvList = DataEncoder.parseDerTLvs(valueBytes)
            return IdbMessageGroup(derTlvList)
        }
    }
}
