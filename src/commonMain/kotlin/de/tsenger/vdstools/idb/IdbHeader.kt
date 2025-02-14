package de.tsenger.vdstools.idb


import de.tsenger.vdstools.DataEncoder
import okio.Buffer


class IdbHeader {
    private var countryIdentifier: ByteArray
    private var signatureAlgorithm: Byte = 0
    var certificateReference: ByteArray? = null
        private set
    private var signatureCreationDate: ByteArray? = null

    private constructor(
        countryIdentifier: ByteArray, signatureAlgorithm: Byte, certificateReference: ByteArray,
        signatureCreationDate: ByteArray
    ) {
        this.countryIdentifier = countryIdentifier
        this.signatureAlgorithm = signatureAlgorithm
        this.certificateReference = certificateReference
        this.signatureCreationDate = signatureCreationDate
    }

    private constructor(
        countryIdentifier: ByteArray
    ) {
        this.countryIdentifier = countryIdentifier
    }


    constructor(
        countryIdentifier: String,
        signatureAlgorithm: IdbSignatureAlgorithm? = null,
        certificateReference: ByteArray? = null,
        signatureCreationDate: String? = null
    ) {
        require(countryIdentifier.length == 3) { "countryIdentifier must be a 3-letter String" }
        this.countryIdentifier = DataEncoder.encodeC40(countryIdentifier)
        if (signatureAlgorithm != null) this.signatureAlgorithm = signatureAlgorithm.value
        this.certificateReference = certificateReference
        if (signatureCreationDate != null) this.signatureCreationDate =
            DataEncoder.encodeMaskedDate(signatureCreationDate)
    }

    fun getCountryIdentifier(): String {
        return DataEncoder.decodeC40(countryIdentifier).trim()
    }

    fun getSignatureAlgorithm(): IdbSignatureAlgorithm? {
        if (signatureAlgorithm.toInt() == 0) return null
        return IdbSignatureAlgorithm.valueOf(signatureAlgorithm)
    }

    fun getSignatureCreationDate(): String? {
        if (signatureCreationDate == null) return null
        return DataEncoder.decodeMaskedDate(signatureCreationDate!!)
    }

    val encoded: ByteArray
        get() {
            val buffer = Buffer()
            buffer.write(countryIdentifier)
            if (signatureAlgorithm.toInt() != 0) buffer.writeByte(signatureAlgorithm.toInt())
            certificateReference?.let { buffer.write(it) }
            signatureCreationDate?.let { buffer.write(it) }
            return buffer.readByteArray()
        }

    companion object {
        fun fromByteArray(rawBytes: ByteArray): IdbHeader {
            require(!(rawBytes.size > 12 || rawBytes.size < 2)) { "Header must have a length between 2 and 12 bytes" }
            val rawData = Buffer().write(rawBytes)

            val countryIdentifier: ByteArray = rawData.readByteArray(2)
            if (rawData.exhausted()) return IdbHeader(countryIdentifier)

            val signatureAlgorithm: Byte = rawData.readByte()
            val certificateReference: ByteArray = rawData.readByteArray(5)
            val signatureCreationDate: ByteArray = rawData.readByteArray(4)
            return IdbHeader(countryIdentifier, signatureAlgorithm, certificateReference, signatureCreationDate)
        }
    }
}
