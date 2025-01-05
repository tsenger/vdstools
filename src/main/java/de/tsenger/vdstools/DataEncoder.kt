package de.tsenger.vdstools


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.vds.Feature
import kotlinx.datetime.LocalDate
import org.bouncycastle.util.Arrays
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

object DataEncoder {
    private var featureEncoder: FeatureConverter

    init {
        featureEncoder = FeatureConverter()
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
//    @JvmStatic
//    @Throws(InvalidNameException::class)
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
    @JvmStatic
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
        if (localDate == null) return ByteArray(0)

        val formattedDate: String = String.format(
            "%02d%02d%d", localDate.monthNumber, localDate.dayOfMonth, localDate.year
        )

        val dateInt = formattedDate.toInt()
        return byteArrayOf((dateInt ushr 16).toByte(), (dateInt ushr 8).toByte(), dateInt.toByte())

    }

    /**
     * Encode a LocalDate as described in as described in ICAO TR "Datastructure for
     * Barcode" in six bytes.
     *
     * @param localDatetime LocalDateTime to encode
     * @return local date time encoded in 6 bytes
     */
    @JvmStatic
    fun encodeDateTime(localDatetime: LocalDateTime): ByteArray {
        val pattern = DateTimeFormatter.ofPattern("MMddyyyyHHmmss")
        val formattedDate = localDatetime.format(pattern)
        val dateInt = BigInteger(formattedDate)
        return dateInt.toByteArray()
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
    @JvmStatic
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

    @JvmStatic
    fun encodeC40(dataString: String): ByteArray {
        var dataString = dataString
        var c1: Int
        var c2: Int
        var c3: Int
        var sum: Int
        val out = ByteArrayOutputStream()

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
                    out.write(sum / 256)
                    out.write(sum % 256)
                } else if (i + 1 < len) {
                    // use zero (Shift1) als filler symbol for c3
                    c1 = getC40Value(dataString[i])
                    c2 = getC40Value(dataString[i + 1])
                    sum = (1600 * c1) + (40 * c2) + 1
                    out.write(sum / 256)
                    out.write(sum % 256)
                } else {
                    // two missing chars: add 0xFE (254 = unlatch) and encode as ASCII
                    // (in datamatrix standard, actual encoded value is ASCII value + 1)
                    out.write(254)
                    out.write(toUnsignedInt(dataString[i].code.toByte()) + 1)
                }
            }
        }
        return out.toByteArray()
    }

    private fun getC40Value(c: Char): Int {
        val value = toUnsignedInt(c.code.toByte())
        return if (value == 32) {
            3
        } else if (value >= 48 && value <= 57) {
            value - 44
        } else if (value >= 65 && value <= 90) {
            value - 51
        } else {
            throw IllegalArgumentException("Not a C40 encodable char: " + c + "value: " + value)
        }
    }

    fun toUnsignedInt(value: Byte): Int {
        return (value.toInt() and 0x7F) + (if (value < 0) 128 else 0)
    }

    @JvmStatic
    fun encodeBase256(ba: ByteArray): String {
        val ca = CharArray(ba.size)
        for (i in ba.indices) {
            ca[i] = (ba[i].toInt() and 0xFF).toChar()
        }
        return String(ca)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun zip(bytesToCompress: ByteArray): ByteArray {
        val compressor = Deflater(Deflater.BEST_COMPRESSION)
        val bos = ByteArrayOutputStream()
        val defos = DeflaterOutputStream(bos, compressor)
        defos.write(bytesToCompress)
        defos.finish()
        val compressedBytes = bos.toByteArray()
        bos.close()
        defos.close()
        Logger.d(
            ("Zip ratio " + (bytesToCompress.size.toFloat() / compressedBytes.size.toFloat()) + ", input size "
                    + bytesToCompress.size + ", compressed size " + compressedBytes.size)
        )
        return compressedBytes
    }

    @JvmStatic
    fun setFeatureEncoder(featureEncoder: FeatureConverter) {
        DataEncoder.featureEncoder = featureEncoder
    }

    @JvmStatic
    fun buildCertificateReference(certificateBytes: ByteArray): ByteArray? {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA1", "BC")
            val certSha1 = messageDigest.digest(certificateBytes)
            return Arrays.copyOfRange(certSha1, 15, 20)
        } catch (e: NoSuchAlgorithmException) {
            Logger.e("Failed building Certificate Reference: " + e.message)
            return null
        } catch (e: NoSuchProviderException) {
            Logger.e("Failed building Certificate Reference: " + e.message)
            return null
        }
    }

    fun encodeDerTlv(vdsType: String?, derTlv: DerTlv?): Feature {
        val value = featureEncoder.decodeFeature<Any>(vdsType, derTlv)
        val name = featureEncoder.getFeatureName(vdsType, derTlv)
        val coding = featureEncoder.getFeatureCoding(vdsType, derTlv)
        return Feature(name, value, coding)
    }

    @JvmStatic
    fun getVdsType(documentRef: Int): String? {
        return featureEncoder.getVdsType(documentRef)
    }

    @JvmStatic
    fun getDocumentRef(vdsType: String?): Int {
        return featureEncoder.getDocumentRef(vdsType)
    }

    fun <T> encodeFeature(vdsType: String?, feature: String?, value: T): DerTlv {
        return featureEncoder.encodeFeature(vdsType, feature, value)
    }
}
