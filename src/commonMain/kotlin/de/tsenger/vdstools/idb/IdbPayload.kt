package de.tsenger.vdstools.idb


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataParser
import okio.Buffer


class IdbPayload(
    val idbHeader: IdbHeader,
    val idbMessageGroup: IdbMessageGroup,
    val idbSignerCertificate: IdbSignerCertificate?,
    val idbSignature: IdbSignature?
) {
    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            buffer.write(idbHeader.encoded)
            buffer.write(idbMessageGroup.encoded)
            if (idbSignerCertificate != null) buffer.write(idbMessageGroup.encoded)
            if (idbSignature != null) {
                buffer.write(idbSignature.encoded)
            } else if (idbHeader.getSignatureAlgorithm() != null) {
                Logger.e(
                    "Missing Signature Field! This field should be present if a signature algorithm has been specified in the header."
                )
            }
            return buffer.readByteArray()
        }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromByteArray(rawBytes: ByteArray, isSigned: Boolean): IdbPayload {
            val idbHeader: IdbHeader
            var idbMessageGroup: IdbMessageGroup? = null
            var idbSignerCertificate: IdbSignerCertificate? = null
            var idbSignature: IdbSignature? = null
            var offset = 0
            val headerSize = if (isSigned) 12 else 2
            idbHeader = IdbHeader.fromByteArray(rawBytes.sliceArray(0 until headerSize).also { offset += headerSize })
            val derTlvList = DataParser.parseDerTLvs(rawBytes.sliceArray(offset until rawBytes.size))

            for (derTlv in derTlvList) {
                when (derTlv.tag) {
                    IdbMessageGroup.TAG -> idbMessageGroup = IdbMessageGroup.fromByteArray(derTlv.encoded)
                    IdbSignerCertificate.TAG -> idbSignerCertificate =
                        IdbSignerCertificate.fromByteArray(derTlv.encoded)

                    IdbSignature.TAG -> idbSignature = IdbSignature.fromByteArray(derTlv.encoded)
                    else -> throw IllegalArgumentException(
                        "Found unknown tag ${derTlv.tag.toString(16).padStart(2, '0').uppercase()} in IdbPayload!"
                    )
                }
            }
            if (idbMessageGroup == null) throw IllegalArgumentException("Didn't found a Message!")

            return IdbPayload(idbHeader, idbMessageGroup, idbSignerCertificate, idbSignature)
        }
    }
}
