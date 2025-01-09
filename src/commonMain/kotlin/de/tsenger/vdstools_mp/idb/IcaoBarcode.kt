package de.tsenger.vdstools_mp.idb


import de.tsenger.vdstools_mp.Base32
import de.tsenger.vdstools_mp.DataEncoder
import de.tsenger.vdstools_mp.DataParser

class IcaoBarcode {
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

    val encoded: String
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

    companion object {
        const val BARCODE_IDENTIFIER: String = "NDB1"

        fun fromString(barcodeString: String): Result<IcaoBarcode> {
            val strBuffer = StringBuilder(barcodeString)

            if (!strBuffer.substring(0, 4).matches(BARCODE_IDENTIFIER.toRegex())) {
                return Result.failure(IllegalArgumentException("Didn't found an ICAO Barcode in the given String: $barcodeString"))
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
                payloadBytes = DataParser.unzip(payloadBytes)
            }

            val payload = IdbPayload.fromByteArray(payloadBytes, isSigned)
            return Result.success(IcaoBarcode(barcodeFlag, payload))
        }
    }
}
