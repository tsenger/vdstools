package de.tsenger.vdstools.vds

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.DataEncoder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.Buffer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class VdsHeader {
    private val log = Logger.withTag(this::class.simpleName ?: "")

    var issuingCountry: String = "UTO"
        private set
    var signerIdentifier: String? = null
        private set
    var certificateReference: String? = null
        private set
    var issuingDate: LocalDate? = null
        private set
    var sigDate: LocalDate? = null
        private set
    var docFeatureRef: Byte = 0
        private set
    var docTypeCat: Byte = 0
        private set
    var rawVersion: Byte = 0
        private set

    private constructor()

    private constructor(builder: Builder) {
        this.issuingCountry = builder.issuingCountry
        this.signerIdentifier = builder.signerIdentifier
        this.certificateReference = builder.certificateReference
        this.issuingDate = builder.issuingDate
        this.sigDate = builder.sigDate
        this.docFeatureRef = builder.docFeatureRef
        this.docTypeCat = builder.docTypeCat
        this.rawVersion = builder.rawVersion
    }

    val signerCertRef: String
        /**
         * Returns a string that identifies the signer certificate. The SignerCertRef
         * string is build from Signer Identifier (country code || signer id) and
         * Certificate Reference. The Signer Identifier maps to the signer certificates
         * subject (C || CN) The Certificate Reference will be interpreted as a hex
         * string integer that represents the serial number of the signer certificate.
         * Leading zeros in Certificate Reference will be cut off. e.g. Signer
         * Identifier 'DETS' and CertificateReference '00027' will result in 'DETS27'
         *
         * @return Formated SignerCertRef all UPPERCASE
         */
        get() {
            val certRefInteger = certificateReference?.trimStart('0')?.ifEmpty { "0" }
            return "${signerIdentifier}${certRefInteger}".uppercase()
        }

    val documentRef: Int
        get() = ((docFeatureRef.toInt() and 0xFF) shl 8) + (docTypeCat.toInt() and 0xFF)

    val vdsType: String
        get() {
            val vdsType = DataEncoder.getVdsType(documentRef)
            return vdsType ?: "UNKNOWN"
        }

    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            try {
                buffer.writeByte(DC.toInt())
                buffer.writeByte(rawVersion.toInt())
                buffer.write(DataEncoder.encodeC40(issuingCountry))
                buffer.write(DataEncoder.encodeC40(encodedSignerIdentifierAndCertificateReference))
                buffer.write(DataEncoder.encodeDate(issuingDate))
                buffer.write(DataEncoder.encodeDate(sigDate))
                buffer.writeByte(docFeatureRef.toInt())
                buffer.writeByte(docTypeCat.toInt())
            } catch (e: Exception) {
                log.e("Error while encoding header data: " + e.message)
            }
            return buffer.readByteArray()
        }

    private val encodedSignerIdentifierAndCertificateReference: String
        get() {
            return if (rawVersion.toInt() == 2) {
                "${signerIdentifier.orEmpty()}${
                    certificateReference.orEmpty().padStart(5, ' ')
                }".uppercase().replace(' ', '0')

            } else if (rawVersion.toInt() == 3) {
                "${signerIdentifier.orEmpty()}${
                    certificateReference.orEmpty().length.toString(16).padStart(2, '0')
                }${certificateReference.orEmpty()}".uppercase()
            } else {
                ""
            }
        }

    @OptIn(ExperimentalTime::class)
    class Builder(vdsType: String) {
        var issuingCountry: String = "UTO"
            private set
        var signerIdentifier: String = "UTXX"
            private set
        var certificateReference: String = "12345"
            private set
        var issuingDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            private set
        var sigDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            private set
        var docFeatureRef: Byte = 0
            private set
        var docTypeCat: Byte = 0
            private set
        var rawVersion: Byte = 3
            private set

        init {
            setDocumentType(vdsType)
        }

        fun setIssuingCountry(issuingCountry: String): Builder {
            this.issuingCountry = issuingCountry
            return this
        }

        fun setSignerIdentifier(signerIdentifier: String): Builder {
            this.signerIdentifier = signerIdentifier
            return this
        }

        fun setCertificateReference(certificateReference: String): Builder {
            this.certificateReference = certificateReference
            return this
        }

        fun setIssuingDate(issuingDate: LocalDate): Builder {
            this.issuingDate = issuingDate
            return this
        }

        fun setSigDate(sigDate: LocalDate): Builder {
            this.sigDate = sigDate
            return this
        }

        fun setRawVersion(rawVersion: Int): Builder {
            this.rawVersion = rawVersion.toByte()
            return this
        }

        fun build(): VdsHeader {
            return VdsHeader(this)
        }

//        /**
//         * Get signerIdentifier and certificateReference from given X509Certificate.
//         *
//         * @param x509Cert                      X509Certificate to get the
//         * signerIdentifier and the
//         * certificateReference from
//         * @param setIssuingCountryFromX509Cert If true also build the issuing country
//         * code base on the X509Certificate. It
//         * will take the Country code 'C' and
//         * convert it to a 3-letter country code.
//         * @return updated Builder instance
//         */
//        fun setSignerCertRef(x509Cert: X509Certificate, setIssuingCountryFromX509Cert: Boolean): Builder {
//            var signerCertRef: Pair<String, String>? = null
//            try {
//                signerCertRef = DataEncoder.getSignerCertRef(x509Cert)
//            } catch (e: Exception) {
//                log.e("Couldn't build header, because getSignerCertRef throws error: " + e.message)
//            }
//            this.signerIdentifier = signerCertRef?.first
//            this.certificateReference = signerCertRef?.second
//            if (setIssuingCountryFromX509Cert) {
//                this.issuingCountry = Doc9303CountryCodes.convertToIcaoOrIso3(signerCertRef?.first?.substring(0, 2))
//            }
//            return this
//        }

        private fun setDocumentType(vdsType: String) {
            val docRef = DataEncoder.getDocumentRef(vdsType)
            if (docRef != null) {
                this.docFeatureRef = ((docRef shr 8) and 0xFF).toByte()
                this.docTypeCat = (docRef and 0xFF).toByte()
            }
        }
    }

    companion object {
        const val DC: Byte = 0xDC.toByte()
        private val log = Logger.withTag(this::class.simpleName ?: "")

        @Throws(IllegalArgumentException::class)
        fun fromBuffer(rawdataBuffer: Buffer): VdsHeader {
            // Magic Byte
            val magicByte = rawdataBuffer.readByte()
            if (magicByte != DC) {
                log.e(
                    "Magic Constant mismatch:  ${magicByte.toString(16).padStart(2, '0').uppercase()}, instead of 0xDC"
                )
                throw IllegalArgumentException(
                    "Magic Constant mismatch:  ${magicByte.toString(16).padStart(2, '0').uppercase()}, instead of 0xDC"
                )
            }

            val vdsHeader = VdsHeader()

            vdsHeader.rawVersion = rawdataBuffer.readByte()
            /*
		 * In ICAO spec for "Visual Digital Seals for Non-Electronic Documents" value
		 * 0x02 stands for version 3 (uses fix length of Document Signer Reference: 5
		 * characters) value 0x03 stands for version 4 (uses variable length of Document
		 * Signer Reference) Problem: German "Arrival Attestation Document" uses value
		 * 0x03 for rawVersion 3 and static length of Document Signer Reference.
		 */
            if (!(vdsHeader.rawVersion.toInt() == 0x02 || vdsHeader.rawVersion.toInt() == 0x03)) {
                log.e(
                    "Unsupported rawVersion: ${vdsHeader.rawVersion.toString(16).padStart(2, '0').uppercase()}"
                )
                throw IllegalArgumentException(
                    "Unsupported rawVersion: ${
                        vdsHeader.rawVersion.toString(16).padStart(2, '0').uppercase()
                    }"
                )
            }
            // 2 bytes stores the three-letter country
            vdsHeader.issuingCountry = DataEncoder.decodeC40(rawdataBuffer.readByteArray(2))

            if (vdsHeader.rawVersion.toInt() == 0x03) { // ICAO version 4

                // 4 bytes stores first 6 characters of Signer & Certificate Reference
                val signerIdentifierAndCertRefLength = DataEncoder.decodeC40(rawdataBuffer.readByteArray(4))
                vdsHeader.signerIdentifier = signerIdentifierAndCertRefLength.take(4)
                // the last two characters store the length of the following Certificate
                // Reference
                val certRefLength = signerIdentifierAndCertRefLength.substring(4).toInt(16)
                log.v("version 4: certRefLength: $certRefLength")

                val bytesToDecode = ((certRefLength - 1) / 3 * 2) + 2
                log.v("version 4: bytesToDecode: $bytesToDecode")
                vdsHeader.certificateReference =
                    DataEncoder.decodeC40(rawdataBuffer.readByteArray(bytesToDecode.toLong()))

            } else { // rawVersion=0x02 -> ICAO version 3
                val signerCertRef = DataEncoder.decodeC40(rawdataBuffer.readByteArray(6))
                vdsHeader.signerIdentifier = signerCertRef.take(4)
                vdsHeader.certificateReference = signerCertRef.substring(4)
            }

            vdsHeader.issuingDate = DataEncoder.decodeDate(rawdataBuffer.readByteArray(3))
            vdsHeader.sigDate = DataEncoder.decodeDate(rawdataBuffer.readByteArray(3))
            vdsHeader.docFeatureRef = rawdataBuffer.readByte()
            vdsHeader.docTypeCat = rawdataBuffer.readByte()
            return vdsHeader
        }
    }
}
