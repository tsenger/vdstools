package de.tsenger.vdstools.idb


import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.internal.logE
import okio.Buffer


internal class IdbPayload(
    val idbHeader: IdbHeader,
    val idbMessageGroup: IdbMessageGroup,
    val idbSignerCertificate: IdbSignerCertificate?,
    val idbSignature: IdbSignature?
) {
    private val tag = this::class.simpleName ?: ""
    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            buffer.write(idbHeader.encoded)
            buffer.write(idbMessageGroup.encoded)
            if (idbSignerCertificate != null) buffer.write(idbSignerCertificate.encoded)
            if (idbSignature != null) {
                buffer.write(idbSignature.encoded)
            } else if (idbHeader.getSignatureAlgorithm() != null) {
                logE(tag, "Missing Signature Field! This field should be present if a signature algorithm has been specified in the header.")
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
            val derTlvList = DerTlv.parseAll(rawBytes.sliceArray(offset until rawBytes.size))

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
