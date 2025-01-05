package de.tsenger.vdstools

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDate
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.InflaterOutputStream

/**
 * Created by Tobias Senger on 18.01.2017.
 */
object DataParser {
    /**
     * Returns a byte array of the requested size which contains the number of bytes
     * from the given ByteBuffer beginning at the current pointer of the ByteBuffer.
     *
     * @param buffer The ByteBuffer to get the number of bytes from.
     * @param size   Number of bytes to get from ByteBuffer. Starting from the
     * internal ByteBuffers pointer
     * @return byte array of length 'size' with bytes from ByteBuffer
     */
    @JvmStatic
    fun getFromByteBuffer(buffer: ByteBuffer, size: Int): ByteArray {
        val tmpByteArray = ByteArray(size)
        if (buffer.position() + size <= buffer.capacity()) {
            buffer[tmpByteArray]
        }
        return tmpByteArray
    }

    /**
     * Decodes a byte[] encoded masked date as described in ICAO TR "Datastructure
     * for Barcode". Returns a date string in format yyyy-MM-dd where unknown parts
     * of the date are marked with an 'x'. e.g. 19xx-10-xx
     *
     * @param maskedDateBytes byte array that contains an encoded masked date
     * @return date string where unknown parts of the date are marked with an 'x'
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun decodeMaskedDate(maskedDateBytes: ByteArray): String {
        require(maskedDateBytes.size == 4) { "expected four bytes for masked date decoding" }
        val mask = maskedDateBytes[0]
        val intval = (toUnsignedInt(maskedDateBytes[1]).toLong() * 256 * 256 + toUnsignedInt(
            maskedDateBytes[2]
        ) * 256L + toUnsignedInt(maskedDateBytes[3]))
        val day = ((intval % 1000000) / 10000).toInt()
        val month = (intval / 1000000).toInt()
        val year = (intval % 10000).toInt()
        // MMddyyyy
        val dateCharArray = String.format("%02d%02d%04d", month, day, year).toCharArray()

        for (i in 0..7) {
            val unknownBit = ((mask.toInt() shr (7 - i)) and 1).toByte()
            if (unknownBit.toInt() == 1) {
                dateCharArray[i] = 'x'
            }
        }
        val dateString = String(dateCharArray)
        return dateString.replace("(.{2})(.{2})(.{4})".toRegex(), "$3-$1-$2").lowercase(Locale.getDefault())
    }

    fun decodeDate(dateBytes: ByteArray): LocalDate {
        require(dateBytes.size == 3) { "expected three bytes for date decoding" }

        val intval =
            (toUnsignedInt(dateBytes[0]).toLong() * 256 * 256 + toUnsignedInt(dateBytes[1]) * 256L + toUnsignedInt(
                dateBytes[2]
            ))
        val day = ((intval % 1000000) / 10000).toInt()
        val month = (intval / 1000000).toInt()
        val year = (intval % 10000).toInt()

        return LocalDate(year, month, day)
    }

    /**
     * Decodes a byte[] encoded datetime as described in ICAO TR "Datastructure for
     * Barcode". Returns a LocalDateTime object
     *
     * @param dateTimeBytes byte array with length 6 which contains encoded datetime
     * @return LocalDateTime object
     */
    @JvmStatic
    fun decodeDateTime(dateTimeBytes: ByteArray): LocalDateTime {
        require(dateTimeBytes.size == 6) { "expected three bytes for date decoding" }
        val dateBigInt = BigInteger(dateTimeBytes)
        val pattern = DateTimeFormatter.ofPattern("MMddyyyyHHmmss")
        return LocalDateTime.parse(String.format("%014d", dateBigInt), pattern)
    }

    @JvmStatic
    fun parseDerTLvs(rawBytes: ByteArray): List<DerTlv> {
        val rawData = ByteBuffer.wrap(rawBytes)
        val derTlvList: MutableList<DerTlv> = ArrayList()
        while (rawData.hasRemaining()) {
            val tag = rawData.get()

            var le = rawData.get().toInt() and 0xff
            if (le == 0x81) {
                le = rawData.get().toInt() and 0xff
            } else if (le == 0x82) {
                le = ((rawData.get().toInt() and 0xff) * 0x100) + (rawData.get().toInt() and 0xff)
            } else if (le == 0x83) {
                le = ((rawData.get().toInt() and 0xff) * 0x1000) + ((rawData.get()
                    .toInt() and 0xff) * 0x100) + (rawData.get().toInt() and 0xff)
            } else if (le > 0x7F) {
                Logger.e(String.format("can't decode length: 0x%02X", le))
                throw IllegalArgumentException(String.format("can't decode length: 0x%02X", le))
            }
            val `val` = getFromByteBuffer(rawData, le)
            derTlvList.add(DerTlv(tag, `val`))
        }
        return derTlvList
    }

    private fun toUnsignedInt(value: Byte): Int {
        return (value.toInt() and 0x7F) + (if (value < 0) 128 else 0)
    }

    @JvmStatic
    fun decodeC40(bytes: ByteArray): String {
        val sb = StringBuilder()

        for (idx in bytes.indices) {
            if (idx % 2 == 0) {
                val i1 = bytes[idx]
                val i2 = bytes[idx + 1]

                if (i1 == 0xFE.toByte()) {
                    sb.append((i2 - 1).toChar())
                } else {
                    var v16 = (toUnsignedInt(i1) shl 8) + toUnsignedInt(i2) - 1
                    var temp = v16 / 1600
                    val u1 = temp
                    v16 -= temp * 1600
                    temp = v16 / 40
                    val u2 = temp
                    val u3 = v16 - temp * 40

                    if (u1 != 0) {
                        sb.append(toChar(u1))
                    }
                    if (u2 != 0) {
                        sb.append(toChar(u2))
                    }
                    if (u3 != 0) {
                        sb.append(toChar(u3))
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun toChar(intValue: Int): Char {
        if (intValue == 3) {
            return 32.toChar()
        } else if (intValue >= 4 && intValue <= 13) {
            return (intValue + 44).toChar()
        } else if (intValue >= 14 && intValue <= 39) {
            return (intValue + 51).toChar()
        }

        // if character is unknown return "?"
        return 63.toChar()
    }

    fun decodeBase256(s: String): ByteArray {
        val ca = s.toCharArray()
        val ba = ByteArray(ca.size)
        for (i in ba.indices) {
            ba[i] = ca[i].code.toByte()
        }
        return ba
    }

    @JvmStatic
    @Throws(IOException::class)
    fun unzip(bytesToDecompress: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val infos = InflaterOutputStream(bos)
        infos.write(bytesToDecompress)
        infos.finish()
        val decompressedBytes = bos.toByteArray()
        bos.close()
        infos.close()
        return decompressedBytes
    }
}
