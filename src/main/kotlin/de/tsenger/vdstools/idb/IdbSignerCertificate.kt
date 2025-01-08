package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class IdbSignerCertificate(cert: X509Certificate) {
    val x509Certificate: X509Certificate = cert

    @get:Throws(CertificateEncodingException::class, IOException::class)
    val encoded: ByteArray
        get() = DerTlv(TAG, x509Certificate.encoded).encoded

    companion object {
        const val TAG: Byte = 0x7E
        fun fromByteArray(rawBytes: ByteArray): IdbSignerCertificate {
            require(rawBytes[0] == TAG) {
                String.format(
                    "IdbSignerCertificate shall have tag %2X, but tag %2X was found instead.",
                    TAG,
                    rawBytes[0]
                )
            }
            val derTlv = DerTlv.fromByteArray(rawBytes)
            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(derTlv!!.value)) as X509Certificate
            return IdbSignerCertificate(cert)
        }
    }
}
