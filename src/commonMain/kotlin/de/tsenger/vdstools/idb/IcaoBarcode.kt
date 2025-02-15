package de.tsenger.vdstools.idb


import de.tsenger.vdstools.Base32
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.Seal

class IcaoBarcode : Seal {
    private var barcodeFlag: Char = 0x41.toChar()
    var payLoad: IdbPayload

    constructor(barcodeFlag: Char, barcodePayload: IdbPayload) {
        this.barcodeFlag = barcodeFlag
        this.payLoad = barcodePayload
    }

    constructor(isSigned: Boolean, isZipped: Boolean, barcodePayload: IdbPayload) {
        if (isSigned) barcodeFlag = (barcodeFlag.code + 0x01).toChar()
        if (isZipped) barcodeFlag = (barcodeFlag.code + 0x02).toChar()
        this.payLoad = barcodePayload
    }

    val isSigned: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x01) == 0x01

    val isZipped: Boolean
        get() = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x02) == 0x02

    override val rawString: String
        get() {
            val strBuffer = StringBuilder(BARCODE_IDENTIFIER)
            strBuffer.append(barcodeFlag)

            val payloadBytes: ByteArray = if (isZipped) {
                DataEncoder.zip(payLoad.encoded)
            } else {
                payLoad.encoded
            }

            val base32EncodedPayload = Base32.encode(payloadBytes).replace("=", "")

            strBuffer.append(base32EncodedPayload)
            return strBuffer.toString()
        }


    override fun getMessage(name: String): Message? {
        return payLoad.idbMessageGroup.getMessage(name) as Message
    }

    override fun getMessage(tag: Int): Message? {
        return payLoad.idbMessageGroup.getMessage(tag) as Message
    }

    override fun getPlainSignature(): ByteArray? {
        return payLoad.idbSignature?.plainSignatureBytes
    }


    val signature: IdbSignature?
        get() {
            return payLoad.idbSignature
        }

    val countryIdentifier: String
        get() {
            return payLoad.idbHeader.getCountryIdentifier()
        }

    val signatureAlgorithmName: String?
        get() {
            return payLoad.idbHeader.getSignatureAlgorithm()?.name
        }

    val signatureCreationDate: String?
        get() {
            return payLoad.idbHeader.getSignatureCreationDate()
        }

    companion object {
        const val BARCODE_IDENTIFIER: String = "NDB1"

        @Throws(IllegalArgumentException::class)
        fun fromString(barcodeString: String): Seal {
            val strBuffer = StringBuilder(barcodeString)

            if (!strBuffer.substring(0, 4).matches(BARCODE_IDENTIFIER.toRegex())) {
                throw IllegalArgumentException("Didn't found an ICAO Barcode in the given String: $barcodeString")
            }

            val barcodeFlag = strBuffer[4]
            val isSigned = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x01) == 0x01
            val isZipped = (((barcodeFlag.code.toByte()) - 0x41).toByte().toInt() and 0x02) == 0x02

            val base32EncodedPayload = StringBuilder(strBuffer.substring(5))
            while (base32EncodedPayload.length % 8 != 0) {
                base32EncodedPayload.append("=")
            }

            var payloadBytes = Base32.decode(base32EncodedPayload.toString())

            if (isZipped) {
                payloadBytes = DataEncoder.unzip(payloadBytes)
            }

            val payload = IdbPayload.fromByteArray(payloadBytes, isSigned)
            return IcaoBarcode(barcodeFlag, payload)
        }
    }
}
