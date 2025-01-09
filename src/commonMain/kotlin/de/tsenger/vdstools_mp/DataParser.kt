package de.tsenger.vdstools_mp

import co.touchlab.kermit.Logger
import de.tsenger.vdstools_mp.asn1.DerTlv
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import okio.Buffer
import okio.Inflater
import okio.InflaterSource

object DataParser {


    /**
     * Decodes a byte[] encoded masked date as described in ICAO TR "Datastructure
     * for Barcode". Returns a date string in format yyyy-MM-dd where unknown parts
     * of the date are marked with an 'x'. e.g. 19xx-10-xx
     *
     * @param maskedDateBytes byte array that contains an encoded masked date
     * @return date string where unknown parts of the date are marked with an 'x'
     */

    fun decodeMaskedDate(maskedDateBytes: ByteArray): String {
        require(maskedDateBytes.size == 4) { "expected four bytes for masked date decoding" }
        val mask = maskedDateBytes[0]
        val intval = (toUnsignedInt(maskedDateBytes[1]).toLong() * 256 * 256 + toUnsignedInt(
            maskedDateBytes[2]
        ) * 256L + toUnsignedInt(maskedDateBytes[3]))
        val day = ((intval % 1000000) / 10000).toInt()
        val month = (intval / 1000000).toInt()
        val year = (intval % 10000).toInt()
        // Pattern: MMddyyyy
        val dateCharArray = String.format("%02d%02d%04d", month, day, year).toCharArray()

        for (i in 0..7) {
            val unknownBit = ((mask.toInt() shr (7 - i)) and 1).toByte()
            if (unknownBit.toInt() == 1) {
                dateCharArray[i] = 'x'
            }
        }
        val dateString = String(dateCharArray)
        return dateString.replace("(.{2})(.{2})(.{4})".toRegex(), "$3-$1-$2").lowercase()
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
    fun decodeDateTime(dateTimeBytes: ByteArray): LocalDateTime {
        require(dateTimeBytes.size == 6) { "Expected six bytes for date decoding" }

        var dateTimeLong = 0L
        for (byte in dateTimeBytes) {
            dateTimeLong = (dateTimeLong shl 8) or (byte.toLong() and 0xFF)
        }

        val paddedDateString = dateTimeLong.toString().padStart(14, '0')

        val month = paddedDateString.substring(0, 2).toInt()
        val day = paddedDateString.substring(2, 4).toInt()
        val year = paddedDateString.substring(4, 8).toInt()
        val hour = paddedDateString.substring(8, 10).toInt()
        val minute = paddedDateString.substring(10, 12).toInt()
        val second = paddedDateString.substring(12, 14).toInt()

        return LocalDateTime(year, month, day, hour, minute, second)
    }

    fun parseDerTLvs(rawBytes: ByteArray): List<DerTlv> {
        val dataBuffer = Buffer().write(rawBytes)
        val derTlvList: MutableList<DerTlv> = ArrayList()
        while (!dataBuffer.exhausted()) {
            val tag = dataBuffer.readByte()

            var le = dataBuffer.readByte().toInt() and 0xff
            if (le == 0x81) {
                le = dataBuffer.readByte().toInt() and 0xff
            } else if (le == 0x82) {
                le = ((dataBuffer.readByte().toInt() and 0xff) * 0x100) + (dataBuffer.readByte().toInt() and 0xff)
            } else if (le == 0x83) {
                le = ((dataBuffer.readByte().toInt() and 0xff) * 0x1000) + ((dataBuffer.readByte()
                    .toInt() and 0xff) * 0x100) + (dataBuffer.readByte().toInt() and 0xff)
            } else if (le > 0x7F) {
                Logger.e(String.format("can't decode length: 0x%02X", le))
                throw IllegalArgumentException(String.format("can't decode length: 0x%02X", le))
            }
            val value = dataBuffer.readByteArray(le.toLong())
            derTlvList.add(DerTlv(tag, value))
        }
        return derTlvList
    }

    private fun toUnsignedInt(value: Byte): Int {
        return (value.toInt() and 0x7F) + (if (value < 0) 128 else 0)
    }

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
        return when (intValue) {
            3 -> {
                32.toChar()
            }

            in 4..13 -> {
                (intValue + 44).toChar()
            }

            in 14..39 -> {
                (intValue + 51).toChar()
            }

            // if character is unknown return "?"
            else -> 63.toChar()
        }
    }

    fun decodeBase256(s: String): ByteArray {
        val ca = s.toCharArray()
        val ba = ByteArray(ca.size)
        for (i in ba.indices) {
            ba[i] = ca[i].code.toByte()
        }
        return ba
    }

    fun unzip(bytesToDecompress: ByteArray): ByteArray {
        val inputBuffer = Buffer().write(bytesToDecompress)
        val inflaterSource = InflaterSource(inputBuffer, Inflater())
        val decompressedBytes = Buffer().apply { writeAll(inflaterSource) }.readByteArray()
        return decompressedBytes
    }
}
