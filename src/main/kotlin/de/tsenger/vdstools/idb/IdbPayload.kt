package de.tsenger.vdstools.idb


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataParser
import java.io.ByteArrayOutputStream
import java.util.*

class IdbPayload(
    val idbHeader: IdbHeader,
    val idbMessageGroup: IdbMessageGroup?,
    val idbSignerCertificate: IdbSignerCertificate?,
    val idbSignature: IdbSignature?
) {
    val encoded: ByteArray
        get() {
            val bos = ByteArrayOutputStream()
            bos.write(idbHeader.encoded)
            bos.write(idbMessageGroup!!.encoded)
            if (idbSignerCertificate != null) bos.write(idbMessageGroup.encoded)
            if (idbSignature != null) {
                bos.write(idbSignature.encoded)
            } else if (idbHeader.getSignatureAlgorithm() != null) {
                Logger.e(
                    "Missing Signature Field! This field should be present if a signature algorithm has been specified in the header."
                )
            }
            return bos.toByteArray()
        }

    companion object {
        fun fromByteArray(rawBytes: ByteArray, isSigned: Boolean): IdbPayload {
            val idbHeader: IdbHeader
            var idbMessageGroup: IdbMessageGroup? = null
            var idbSignerCertificate: IdbSignerCertificate? = null
            var idbSignature: IdbSignature? = null
            var offset = 0
            if (isSigned) {
                idbHeader =
                    IdbHeader.fromByteArray(Arrays.copyOfRange(rawBytes, offset, 12.let { offset += it; offset }))
            } else {
                idbHeader =
                    IdbHeader.fromByteArray(Arrays.copyOfRange(rawBytes, offset, 2.let { offset += it; offset }))
            }
            val derTlvList = DataParser.parseDerTLvs(Arrays.copyOfRange(rawBytes, offset, rawBytes.size))

            for (derTlv in derTlvList) {
                when (derTlv.tag) {
                    IdbMessageGroup.TAG -> idbMessageGroup = IdbMessageGroup.fromByteArray(derTlv.encoded)
                    IdbSignerCertificate.TAG -> idbSignerCertificate =
                        IdbSignerCertificate.fromByteArray(derTlv.encoded)

                    IdbSignature.TAG -> idbSignature = IdbSignature.fromByteArray(derTlv.encoded)
                    else -> throw IllegalArgumentException(
                        String.format("Found unknown tag %2X in IdbPayload!", derTlv.tag)
                    )
                }
            }

            return IdbPayload(idbHeader, idbMessageGroup, idbSignerCertificate, idbSignature)
        }
    }
}
