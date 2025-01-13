package de.tsenger.vdstools_mp


import co.touchlab.kermit.Logger
import de.tsenger.vdstools_mp.asn1.DerTlv
import de.tsenger.vdstools_mp.vds.Feature
import de.tsenger.vdstools_mp.vds.FeatureCoding
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA1
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import okio.Buffer
import okio.Deflater
import okio.DeflaterSink
import okio.FileNotFoundException

//import org.kotlincrypto.hash.sha1.SHA1


object DataEncoder {
    private lateinit var featureEncoder: FeatureConverter

    init {
        val filerLoader = FileLoader()
        try {
            val jsonString = filerLoader.loadFileFromResources(DEFAULT_SEAL_CODINGS)
            featureEncoder = FeatureConverter(jsonString)
        }
        catch (e: FileNotFoundException) {
            Logger.e("Can't initialize FeatureEncoder: ${e.message}")
            println("Can't initialize FeatureEncoder: ${e.message}")
        }
    }


//    /**
//     * Return the Signer Identifier and the Certificate Reference based on the
//     * given X.509. Signer Identifier is C + CN Certificate Reference is the serial
//     * number of the X509Certificate. It will be encoded as hex string
//     *
//     * @param cert X509 certificate to get the signer information from
//     * @return String array that contains the signerIdentifier at index 0 and
//     * CertRef at index 1
//     * @throws InvalidNameException if a syntax violation is detected.
//     */
//    fun getSignerCertRef(cert: X509Certificate): Pair<String, String> {
//    //TODO use new X509-Lib correct
//        val ln = LdapName(cert.subjectX500Principal.name)
//
//        var c = ""
//        var cn = ""
//        for (rdn in ln.rdns) {
//            if (rdn.type.equals("CN", ignoreCase = true)) {
//                cn = rdn.value as String
//                Logger.d("CN is: $cn")
//            } else if (rdn.type.equals("C", ignoreCase = true)) {
//                c = rdn.value as String
//                Logger.d("C is: $c")
//            }
//        }
//
//        val ccn = String.format("%s%s", c, cn).uppercase()
//        val serial = cert.serialNumber.toString(16) // Serial Number as Hex
//        val signerCertRef = Pair(ccn, serial)
//
//        Logger.i("generated signerCertRef: " + signerCertRef.first + signerCertRef.second)
//        return signerCertRef
//
//    }

    /**
     * @param dateString Date as String formated as yyyy-MM-dd
     * @return date encoded in 3 bytes
     */
    fun encodeDate(dateString: String): ByteArray {
        val dt: LocalDate = LocalDate.parse(dateString)
        return encodeDate(dt)
    }

    /**
     * Encode a LocalDate as described in ICAO Doc9303 Part 13 in three bytes
     *
     * @param localDate Date
     * @return date encoded in 3 bytes
     */
    fun encodeDate(localDate: LocalDate?): ByteArray {
        if (localDate == null) return ByteArray(3)
        val formattedDate: String = localDate.monthNumber.toString().padStart(2, '0') +
                localDate.dayOfMonth.toString().padStart(2, '0') +
                localDate.year.toString().padStart(4, '0')
        val dateInt = formattedDate.toInt()
        return numberToByteArray(dateInt)

    }


    /**
     * Encode a LocalDate as described in as described in ICAO TR "Datastructure for
     * Barcode" in six bytes.
     *
     * @param localDatetime LocalDateTime to encode
     * @return local date time encoded in 6 bytes
     */
    fun encodeDateTime(localDatetime: LocalDateTime): ByteArray {
        val formattedDateTime: String =
            localDatetime.monthNumber.toString().padStart(2, '0') +
                    localDatetime.dayOfMonth.toString().padStart(2, '0') +
                    localDatetime.year.toString().padStart(4, '0') +
                    localDatetime.hour.toString().padStart(2, '0') +
                    localDatetime.minute.toString().padStart(2, '0') +
                    localDatetime.second.toString().padStart(2, '0')

        val dateInt = formattedDateTime.toLong()
        return numberToByteArray(dateInt)

    }

    private fun numberToByteArray(value: Number): ByteArray {
        return when (value) {
            is Long -> {
                // Konvertiere Long zu ByteArray (8 Bytes), beschränke auf 6 und ergänze führende Nullen
                val byteArray = ByteArray(8) { i ->
                    (value shr (8 * (7 - i)) and 0xFF).toByte()
                }.takeLast(6).toByteArray()
                // Falls kürzer als 6 Bytes, mit führenden Nullen auffüllen
                ByteArray(6 - byteArray.size) { 0 } + byteArray
            }

            is Int -> {
                // Konvertiere Int zu ByteArray (4 Bytes), beschränke auf 3 und ergänze führende Nullen
                val byteArray = ByteArray(4) { i ->
                    (value shr (8 * (3 - i)) and 0xFF).toByte()
                }.takeLast(3).toByteArray()
                // Falls kürzer als 3 Bytes, mit führenden Nullen auffüllen
                ByteArray(3 - byteArray.size) { 0 } + byteArray
            }

            else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
        }
    }

    /**
     * Encodes a date string with unknown date parts as described in ICAO TR
     * "Datastructure for Barcode". Unknown parts of the date string shall be filled
     * with an 'x', e.g. 19xx-10-xx
     *
     * @param dateString date as String formated as yyyy-MM-dd where unknown parts
     * could be replaced by an x
     * @return masked date encoded in 4 bytes
     */

    fun encodeMaskedDate(dateString: String): ByteArray {
        require(dateString.matches("(.{4})-(.{2})-(.{2})".toRegex())) { "Date string must be formated as yyyy-MM-dd." }

        val formattedDate =
            dateString.replace("(.{4})-(.{2})-(.{2})".toRegex(), "$2$3$1").lowercase()
        val dateInt = formattedDate.replace("x".toRegex(), "0").toInt()
        val dateCharArray = formattedDate.toCharArray()

        var mask: Byte = 0
        for (i in 0..7) {
            if (dateCharArray[i] == 'x') {
                mask = (mask.toInt() or (0x80 shr i)).toByte()
            }
        }
        return byteArrayOf(mask, (dateInt ushr 16).toByte(), (dateInt ushr 8).toByte(), dateInt.toByte())
    }

    fun encodeC40(string: String): ByteArray {
        var dataString = string
        var c1: Int
        var c2: Int
        var c3: Int
        var sum: Int
        val out = Buffer()

        dataString = dataString.uppercase().replace("<".toRegex(), " ")

        val len = dataString.length

        for (i in 0..<len) {
            if (i % 3 == 0) {
                if (i + 2 < len) {
                    // encode standard way
                    c1 = getC40Value(dataString[i])
                    c2 = getC40Value(dataString[i + 1])
                    c3 = getC40Value(dataString[i + 2])
                    sum = (1600 * c1) + (40 * c2) + c3 + 1
                    out.writeByte(sum / 256)
                    out.writeByte(sum % 256)
                } else if (i + 1 < len) {
                    // use zero (Shift1) als filler symbol for c3
                    c1 = getC40Value(dataString[i])
                    c2 = getC40Value(dataString[i + 1])
                    sum = (1600 * c1) + (40 * c2) + 1
                    out.writeByte(sum / 256)
                    out.writeByte(sum % 256)
                } else {
                    // two missing chars: add 0xFE (254 = unlatch) and encode as ASCII
                    // (in datamatrix standard, actual encoded value is ASCII value + 1)
                    out.writeByte(254)
                    out.writeByte(toUnsignedInt(dataString[i].code.toByte()) + 1)
                }
            }
        }
        return out.readByteArray()
    }

    private fun getC40Value(c: Char): Int {
        return when (val value = toUnsignedInt(c.code.toByte())) {
            32 -> {
                3
            }

            in 48..57 -> {
                value - 44
            }

            in 65..90 -> {
                value - 51
            }

            else -> {
                throw IllegalArgumentException("Not a C40 encodable char: " + c + "value: " + value)
            }
        }
    }

    private fun toUnsignedInt(value: Byte): Int {
        return (value.toInt() and 0x7F) + (if (value < 0) 128 else 0)
    }


    fun encodeBase256(ba: ByteArray): String {
        val ca = CharArray(ba.size)
        for (i in ba.indices) {
            ca[i] = (ba[i].toInt() and 0xFF).toChar()
        }
        return ca.concatToString()
    }

    fun zip(bytesToCompress: ByteArray): ByteArray {
        val outputBuffer = Buffer()
        val inputBuffer = Buffer().write(bytesToCompress)
        val compressor = DeflaterSink(outputBuffer, Deflater(9, false))
        compressor.write(inputBuffer, inputBuffer.size)
        compressor.close()
        val compressedBytes = outputBuffer.readByteArray()

        Logger.d(
            ("Zip ratio " + (bytesToCompress.size.toFloat() / compressedBytes.size.toFloat()) + ", input size "
                    + bytesToCompress.size + ", compressed size " + compressedBytes.size)
        )
        return compressedBytes
    }

    fun setFeatureEncoder(featureEncoder: FeatureConverter) {
        DataEncoder.featureEncoder = featureEncoder
    }


    @OptIn(DelicateCryptographyApi::class)
    fun buildCertificateReference(certificateBytes: ByteArray): ByteArray {
        val hasher = CryptographyProvider.Default
            .get(SHA1)
            .hasher()

        var certSha1 = runBlocking {
            hasher.hash(certificateBytes)
        }
        return certSha1.sliceArray(15..19)
    }

    fun encodeDerTlv(vdsType: String, derTlv: DerTlv): Feature? {
        val value = featureEncoder.decodeFeature<Any>(vdsType, derTlv)
        val name = featureEncoder.getFeatureName(vdsType, derTlv)
        val coding = featureEncoder.getFeatureCoding(vdsType, derTlv)
        if (name == "" || coding == FeatureCoding.UNKNOWN) return null
        return Feature(name, value, coding)
    }


    fun getVdsType(documentRef: Int): String? {
        return featureEncoder.getVdsType(documentRef)
    }

    fun getDocumentRef(vdsType: String): Int? {
        return featureEncoder.getDocumentRef(vdsType)
    }

    fun <T> encodeFeature(vdsType: String, feature: String, value: T): DerTlv {
        return featureEncoder.encodeFeature(vdsType, feature, value)
    }
}
