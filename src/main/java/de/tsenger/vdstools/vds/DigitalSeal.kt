package de.tsenger.vdstools.vds


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.DataParser
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.asn1.DerTlv
import kotlinx.datetime.LocalDate
import okio.Buffer
import java.io.IOException


class DigitalSeal {
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

    val issuingCountry: String?
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

    @get:Throws(IOException::class)
    val encoded: ByteArray
        get() = vdsHeader.encoded + vdsMessage.encoded + (vdsSignature?.encoded ?: ByteArray(0))

    val signatureBytes: ByteArray
        get() = vdsSignature!!.plainSignatureBytes

    @get:Throws(IOException::class)
    val rawString: String
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
            Logger.e("Signature creation failed: " + e.localizedMessage)
            return null
        }
    }


    companion object {

        @JvmStatic
        fun fromRawString(rawString: String): DigitalSeal? {
            var seal: DigitalSeal? = null
            try {
                seal = parseVdsSeal(DataParser.decodeBase256(rawString))
            } catch (e: Exception) {
                Logger.e(e.localizedMessage)
            }
            return seal
        }

        @JvmStatic
        fun fromByteArray(rawBytes: ByteArray): DigitalSeal? {
            var seal: DigitalSeal? = null
            try {
                seal = parseVdsSeal(rawBytes)
            } catch (e: Exception) {
                Logger.e(e.localizedMessage)
            }
            return seal
        }

        @OptIn(ExperimentalStdlibApi::class)
        @Throws(IOException::class)
        private fun parseVdsSeal(rawBytes: ByteArray): DigitalSeal {
            val rawDataBuffer = Buffer().write(rawBytes)
            Logger.v("rawData: ${rawBytes.toHexString()}")

            val vdsHeader = VdsHeader.fromBuffer(rawDataBuffer)
            var vdsSignature: VdsSignature? = null

            val derTlvList = DataParser
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
