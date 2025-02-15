package de.tsenger.vdstools.vds


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal
import kotlinx.datetime.LocalDate
import okio.Buffer


class DigitalSeal : Seal {
    private val log = Logger.withTag(this::class.simpleName ?: "")

    val vdsType: String
    private val vdsHeader: VdsHeader
    private val vdsMessage: VdsMessage
    private val vdsSignature: VdsSignature?

    private constructor(vdsHeader: VdsHeader, vdsMessage: VdsMessage, vdsSignature: VdsSignature?) {
        this.vdsHeader = vdsHeader
        this.vdsMessage = vdsMessage
        this.vdsSignature = vdsSignature
        this.vdsType = vdsHeader.vdsType
    }

    constructor(vdsHeader: VdsHeader, vdsMessage: VdsMessage, signer: Signer) {
        this.vdsHeader = vdsHeader
        this.vdsMessage = vdsMessage
        this.vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer)
        this.vdsType = vdsHeader.vdsType
    }

    val issuingCountry: String
        get() = vdsHeader.issuingCountry

    val signerCertRef: String
        get() = vdsHeader.signerCertRef

    val signerIdentifier: String?
        get() = vdsHeader.signerIdentifier

    val certificateReference: String?
        get() = vdsHeader.certificateReference

    val issuingDate: LocalDate?
        get() = vdsHeader.issuingDate

    val sigDate: LocalDate?
        get() = vdsHeader.sigDate

    val docFeatureRef: Byte
        get() = vdsHeader.docFeatureRef

    val docTypeCat: Byte
        get() = vdsHeader.docTypeCat

    val headerAndMessageBytes: ByteArray
        get() = vdsHeader.encoded + vdsMessage.encoded

    val encoded: ByteArray
        get() = vdsHeader.encoded + vdsMessage.encoded + (vdsSignature?.encoded ?: byteArrayOf())

    val signatureBytes: ByteArray
        get() = vdsSignature?.plainSignatureBytes ?: byteArrayOf()

    override val rawString: String
        get() = DataEncoder.encodeBase256(encoded)

    val featureList: List<Feature>
        get() = vdsMessage.featureList

    fun getFeature(feature: String): Feature? {
        return vdsMessage.getFeature(feature)
    }

    private fun createVdsSignature(vdsHeader: VdsHeader, vdsMessage: VdsMessage, signer: Signer): VdsSignature? {
        val headerMessage = vdsHeader.encoded + vdsMessage.encoded
        try {
            val signatureBytes = signer.sign(headerMessage)
            return VdsSignature(signatureBytes)
        } catch (e: Exception) {
            log.e("Signature creation failed: " + e.message)
            return null
        }
    }

    override fun getMessage(name: String): Message? {
        val feature = vdsMessage.getFeature(name)
        return feature?.let { Message(it.tag, it.name, it.valueBytes, it.coding) }
    }

    override fun getMessage(tag: Int): Message? {
        val feature = vdsMessage.getFeature(tag)
        return feature?.let { Message(it.tag, it.name, it.valueBytes, it.coding) }
    }

    override fun getPlainSignature(): ByteArray? {
        return signatureBytes
    }


    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")
        fun fromRawString(rawString: String): Seal {
            var seal: Seal? = null
            try {
                seal = parseVdsSeal(DataEncoder.decodeBase256(rawString))
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
            return seal!!
        }

        fun fromByteArray(rawBytes: ByteArray): Seal {
            var seal: Seal? = null
            try {
                seal = parseVdsSeal(rawBytes)
            } catch (e: Exception) {
                log.e(e.message.toString())
            }
            return seal!!
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun parseVdsSeal(rawBytes: ByteArray): Seal {
            val rawDataBuffer = Buffer().write(rawBytes)
            log.v("rawData: ${rawBytes.toHexString()}")

            val vdsHeader = VdsHeader.fromBuffer(rawDataBuffer)
            var vdsSignature: VdsSignature? = null

            val derTlvList = DataEncoder
                .parseDerTLvs(rawDataBuffer.readByteArray())

            val featureList: MutableList<DerTlv> = ArrayList(derTlvList.size - 1)

            for (derTlv in derTlvList) {
                if (derTlv.tag == 0xff.toByte()) {
                    vdsSignature = VdsSignature.fromByteArray(derTlv.encoded)
                } else {
                    featureList.add(derTlv)
                }
            }
            val vdsMessage = VdsMessage(vdsHeader.vdsType, featureList)
            return DigitalSeal(vdsHeader, vdsMessage, vdsSignature)
        }
    }
}
